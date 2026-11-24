import { useEffect, useState } from "react";

const explicitContentThreshold = 0.5;
let safetyModelPromise;

function loadSafetyModel() {
  if (!safetyModelPromise) {
    safetyModelPromise = Promise.all([
      import("nsfwjs/core"),
      import("nsfwjs/models/mobilenet_v2"),
    ])
      .then(([{ load }, { MobileNetV2Model }]) =>
        load("MobileNetV2", {
          modelDefinitions: [MobileNetV2Model],
        }),
      )
      .catch((error) => {
        safetyModelPromise = null;
        throw error;
      });
  }
  return safetyModelPromise;
}

function getExplicitContentScore(predictions) {
  return predictions
    .filter((item) => item.className === "Porn" || item.className === "Hentai")
    .reduce((total, item) => total + item.probability, 0);
}

function mergeAdultContentPredictions(predictions) {
  const merged = predictions
    .filter((item) => item.className !== "Sexy")
    .map((item) => ({ ...item }));
  const sexyScore = predictions
    .filter((item) => item.className === "Sexy")
    .reduce((total, item) => total + item.probability, 0);

  if (sexyScore > 0) {
    const pornIndex = merged.findIndex((item) => item.className === "Porn");
    if (pornIndex >= 0) {
      merged[pornIndex].probability += sexyScore;
    } else {
      merged.push({ className: "Porn", probability: sexyScore });
    }
  }

  return merged.sort((first, second) => second.probability - first.probability);
}

function getMediaKind(file) {
  const type = String(file?.type || "").toLowerCase();
  if (type.startsWith("image/")) return "image";
  if (type.startsWith("video/")) return "video";

  const extension = String(file?.name || "")
    .split(".")
    .pop()
    ?.toLowerCase();
  if (["avif", "bmp", "gif", "jpeg", "jpg", "png", "webp"].includes(extension))
    return "image";
  if (
    ["avi", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "ogv", "webm"].includes(
      extension,
    )
  ) {
    return "video";
  }
  return "";
}

async function classifyVideo(model, video, url, shouldContinue) {
  video.preload = "auto";
  video.muted = true;
  video.playsInline = true;
  const videoReady = new Promise((resolve, reject) => {
    video.onloadeddata = resolve;
    video.onerror = () =>
      reject(new Error("The selected video could not be decoded."));
  });
  video.src = url;
  await videoReady;

  const duration = Number.isFinite(video.duration) ? video.duration : 0;
  const sampleCount = duration > 0 ? 5 : 1;
  const sampleEnd = Math.max(0, duration - 0.1);
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");
  if (!context || !video.videoWidth || !video.videoHeight) {
    throw new Error("The selected video has no readable frames.");
  }
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;

  const framePredictions = [];
  for (let index = 0; index < sampleCount; index += 1) {
    if (!shouldContinue()) return [];
    const timestamp =
      sampleCount === 1 ? 0 : (sampleEnd * index) / (sampleCount - 1);
    if (Math.abs(video.currentTime - timestamp) > 0.05) {
      await new Promise((resolve, reject) => {
        video.onseeked = resolve;
        video.onerror = () =>
          reject(new Error("A video frame could not be read."));
        video.currentTime = timestamp;
      });
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    framePredictions.push(await model.classify(canvas));
    if (!shouldContinue()) return [];
  }

  const totals = new Map();
  framePredictions.forEach((frame) => {
    frame.forEach((item) => {
      totals.set(
        item.className,
        (totals.get(item.className) || 0) + item.probability,
      );
    });
  });
  return [...totals.entries()]
    .map(([className, total]) => ({
      className,
      probability: total / framePredictions.length,
    }))
    .sort((first, second) => second.probability - first.probability);
}

function releaseMedia(video, url) {
  if (video) {
    video.pause();
    video.removeAttribute("src");
    video.load();
  }
  if (url) URL.revokeObjectURL(url);
}

export function useMediaSafety(file) {
  const [analysis, setAnalysis] = useState(null);
  const kind = getMediaKind(file);
  const isSupported = Boolean(kind);
  const currentAnalysis = analysis?.file === file ? analysis : null;
  const isAnalyzing = isSupported && !currentAnalysis;
  const predictions = currentAnalysis?.predictions || [];
  const error = currentAnalysis?.error || "";
  const isRejected = Boolean(currentAnalysis?.isRejected);

  useEffect(() => {
    if (!isSupported) return undefined;

    let isCurrent = true;
    let video;
    const url = URL.createObjectURL(file);

    async function analyze() {
      try {
        const model = await loadSafetyModel();
        if (!isCurrent) return;
        let results;
        if (kind === "image") {
          const image = new Image();
          image.src = url;
          await image.decode();
          if (!isCurrent) return;
          results = await model.classify(image);
        } else {
          video = document.createElement("video");
          results = await classifyVideo(model, video, url, () => isCurrent);
        }
        if (!isCurrent) return;
        if (!results?.length)
          throw new Error("The content safety scanner returned no results.");

        if (isCurrent) {
          setAnalysis({
            file,
            predictions: mergeAdultContentPredictions(results),
            isRejected:
              getExplicitContentScore(results) >= explicitContentThreshold,
            error: "",
          });
        }
      } catch (scanError) {
        console.error("Error analyzing selected media:", scanError);
        if (isCurrent) {
          setAnalysis({
            file,
            predictions: [],
            isRejected: false,
            error:
              "This media could not be scanned. Remove it or try another file.",
          });
        }
      } finally {
        releaseMedia(video, url);
      }
    }

    analyze();
    return () => {
      isCurrent = false;
      releaseMedia(video, url);
    };
  }, [file, isSupported, kind]);

  return {
    kind,
    isSupported,
    isAnalyzing,
    isRejected,
    predictions,
    error,
    isBlocked: Boolean(
      isSupported &&
      (isAnalyzing || error || isRejected || predictions.length === 0),
    ),
  };
}
