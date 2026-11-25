import { useEffect, useState } from "react";

// ========================================
// 1. CONTENT MODERATION CONFIGURATION
// ========================================

const CONTENT_THRESHOLDS = {
  porn: 0.65,
  hentai: 0.65,

  // High-confidence single-frame safeguard
  singleFrame: 0.95,
};

const VIDEO_SAMPLE_COUNT = 10;

let safetyModelPromise;

// ========================================
// 2. LOAD NSFWJS MODEL
// ========================================

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

// ========================================
// 3. EXPLICIT CONTENT DETECTION
// ========================================

function getExplicitContentScore(predictions) {
  const porn = predictions.find((item) => item.className === "Porn");

  const hentai = predictions.find((item) => item.className === "Hentai");

  const pornScore = porn?.probability || 0;
  const hentaiScore = hentai?.probability || 0;

  const pornMax = porn?.maxProbability ?? pornScore;
  const hentaiMax = hentai?.maxProbability ?? hentaiScore;

  const isExplicit =
    pornScore >= CONTENT_THRESHOLDS.porn ||
    hentaiScore >= CONTENT_THRESHOLDS.hentai ||
    pornMax >= CONTENT_THRESHOLDS.singleFrame ||
    hentaiMax >= CONTENT_THRESHOLDS.singleFrame;

  return {
    pornScore,
    hentaiScore,
    isExplicit,
  };
}

// ========================================
// 4. MERGE PREDICTIONS
// Sexy is NOT converted into Porn
// ========================================

function mergeAdultContentPredictions(predictions) {
  return [...predictions].sort((a, b) => b.probability - a.probability);
}

// ========================================
// 5. DETECT IMAGE OR VIDEO
// ========================================

export function getMediaKind(file) {
  const type = String(file?.type || "").toLowerCase();

  if (type.startsWith("image/")) return "image";

  if (type.startsWith("video/")) return "video";

  const extension = String(file?.name || "")
    .split(".")
    .pop()
    ?.toLowerCase();

  if (
    ["avif", "bmp", "gif", "jpeg", "jpg", "png", "webp"].includes(extension)
  ) {
    return "image";
  }

  if (
    ["avi", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "ogv", "webm"].includes(
      extension,
    )
  ) {
    return "video";
  }

  return "";
}

// ========================================
// 6. VIDEO CLASSIFICATION
// ========================================

async function classifyVideo(model, video, url, shouldContinue) {
  video.preload = "auto";
  video.muted = true;
  video.playsInline = true;

  const videoReady = new Promise((resolve, reject) => {
    video.onloadeddata = resolve;

    video.onerror = () => {
      reject(new Error("The selected video could not be decoded."));
    };
  });

  video.src = url;

  await videoReady;

  const duration = Number.isFinite(video.duration) ? video.duration : 0;

  const sampleCount = duration > 0 ? VIDEO_SAMPLE_COUNT : 1;

  const sampleEnd = Math.max(0, duration - 0.1);

  const canvas = document.createElement("canvas");

  const context = canvas.getContext("2d");

  if (!context || !video.videoWidth || !video.videoHeight) {
    throw new Error("The selected video has no readable frames.");
  }

  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;

  const framePredictions = [];

  // Analyze video frames

  for (let index = 0; index < sampleCount; index += 1) {
    if (!shouldContinue()) return [];

    const timestamp =
      sampleCount === 1 ? 0 : (sampleEnd * index) / (sampleCount - 1);

    if (Math.abs(video.currentTime - timestamp) > 0.05) {
      await new Promise((resolve, reject) => {
        video.onseeked = resolve;

        video.onerror = () => {
          reject(new Error("A video frame could not be read."));
        };

        video.currentTime = timestamp;
      });
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    const predictions = await model.classify(canvas);

    framePredictions.push(predictions);

    if (!shouldContinue()) return [];
  }

  // Calculate average and maximum scores
  // separately for each category.

  const totals = new Map();

  const maximums = new Map();

  framePredictions.forEach((frame) => {
    frame.forEach((item) => {
      const previous = totals.get(item.className) || 0;

      totals.set(item.className, previous + item.probability);

      const maxPrevious = maximums.get(item.className) || 0;

      maximums.set(item.className, Math.max(maxPrevious, item.probability));
    });
  });

  return [...totals.entries()]
    .map(([className, total]) => ({
      className,

      probability: total / framePredictions.length,

      maxProbability: maximums.get(className) || 0,
    }))
    .sort((a, b) => b.probability - a.probability);
}

// ========================================
// 7. RELEASE MEDIA RESOURCES
// ========================================

function releaseMedia(video, url) {
  if (video) {
    video.pause();
    video.removeAttribute("src");
    video.load();
  }

  if (url) {
    URL.revokeObjectURL(url);
  }
}

export async function analyzeMediaFile(file, shouldContinue = () => true) {
  const kind = getMediaKind(file);
  if (!kind) {
    throw new Error("The selected file type cannot be scanned.");
  }

  const url = URL.createObjectURL(file);
  let video;

  try {
    const model = await loadSafetyModel();
    if (!shouldContinue()) return null;

    let results = [];

    if (kind === "image") {
      const image = new Image();
      image.src = url;
      await image.decode();

      if (!shouldContinue()) return null;
      results = await model.classify(image);
    } else {
      video = document.createElement("video");
      results = await classifyVideo(model, video, url, shouldContinue);
    }

    if (!shouldContinue()) return null;
    if (!results || results.length === 0) {
      throw new Error("The content safety scanner returned no results.");
    }

    const explicitResult = getExplicitContentScore(results);
    return {
      kind,
      isSupported: true,
      isRejected: explicitResult.isExplicit,
      predictions: mergeAdultContentPredictions(results),
    };
  } finally {
    releaseMedia(video, url);
  }
}

// ========================================
// 8. MAIN REACT HOOK
// ========================================

export function useMediaSafety(file) {
  const [analysis, setAnalysis] = useState(null);

  const kind = getMediaKind(file);

  const isSupported = Boolean(kind);

  const currentAnalysis = analysis?.file === file ? analysis : null;

  const isAnalyzing = Boolean(file && isSupported && !currentAnalysis);

  const predictions = currentAnalysis?.predictions || [];

  const error = currentAnalysis?.error || "";

  const isRejected = Boolean(currentAnalysis?.isRejected);

  // ========================================
  // ANALYZE MEDIA
  // ========================================

  useEffect(() => {
    if (!file || !isSupported) {
      return undefined;
    }

    let isCurrent = true;

    async function analyze() {
      try {
        const result = await analyzeMediaFile(file, () => isCurrent);
        if (!isCurrent || !result) return;

        setAnalysis({
          file,
          predictions: result.predictions,
          isRejected: result.isRejected,
          error: "",
        });
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
      }
    }

    analyze();

    return () => {
      isCurrent = false;
    };
  }, [file, isSupported, kind]);

  // ========================================
  // 9. RETURN MODERATION RESULTS
  // ========================================

  return {
    kind,

    isSupported,

    isAnalyzing,

    isRejected,

    predictions,

    error,

    isBlocked: Boolean(
      file &&
      (!isSupported ||
        isAnalyzing ||
        error ||
        isRejected ||
        predictions.length === 0),
    ),
  };
}
