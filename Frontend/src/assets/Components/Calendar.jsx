import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, Check, ChevronLeft, ChevronRight, Plus, Trash2 } from 'lucide-react'
import api from '../../api'
import './Calendar.css'

const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const entryTypes = ['Reminder', 'Task', 'Note']

function toDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function readEntries(storageKey) {
  if (!storageKey) return []

  try {
    const savedEntries = window.localStorage.getItem(storageKey)
    if (!savedEntries) return []
    const parsedEntries = JSON.parse(savedEntries)
    return Array.isArray(parsedEntries) ? parsedEntries : []
  } catch (error) {
    console.warn('Could not load calendar entries:', error)
    return []
  }
}

function formatSelectedDate(dateKey) {
  const date = new Date(`${dateKey}T12:00:00`)
  return date.toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

export default function CalendarPage({ storageKey }) {
  const [today] = useState(() => toDateKey(new Date()))
  const [entries, setEntries] = useState(() => readEntries(storageKey))
  const [selectedDate, setSelectedDate] = useState(today)
  const [visibleMonth, setVisibleMonth] = useState(() => {
    const currentDate = new Date()
    return new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
  })
  const [title, setTitle] = useState('')
  const [type, setType] = useState('Reminder')
  const [time, setTime] = useState('')
  const [details, setDetails] = useState('')
  const [storageError, setStorageError] = useState('')
  const [syncError, setSyncError] = useState('')

  useEffect(() => {
    if (!storageKey) return undefined

    let isCurrent = true
    async function syncCalendarEntries() {
      try {
        const remoteEntries = await api.getCalendarEntries()
        const remoteById = new Map(
          remoteEntries.map((entry) => [String(entry.id), entry]),
        )
        const localEntries = readEntries(storageKey)

        for (const entry of localEntries) {
          if (!remoteById.has(String(entry.id))) {
            const savedEntry = await api.saveCalendarEntry(entry)
            remoteById.set(String(savedEntry.id), savedEntry)
          }
        }

        if (!isCurrent) return
        const latestLocalEntries = readEntries(storageKey)
        const mergedById = new Map(remoteById)
        latestLocalEntries.forEach((entry) => {
          if (!mergedById.has(String(entry.id))) {
            mergedById.set(String(entry.id), entry)
          }
        })
        const mergedEntries = [...mergedById.values()]
        setEntries(mergedEntries)
        window.localStorage.setItem(storageKey, JSON.stringify(mergedEntries))
        setSyncError('')
      } catch (error) {
        if (!isCurrent) return
        console.error('Could not sync calendar entries:', error)
        setSyncError('Calendar reminders could not sync. Your entries remain saved on this device.')
      }
    }

    syncCalendarEntries()
    return () => {
      isCurrent = false
    }
  }, [storageKey])

  function saveEntries(nextEntries) {
    setEntries(nextEntries)
    if (!storageKey) return
    try {
      window.localStorage.setItem(storageKey, JSON.stringify(nextEntries))
      setStorageError('')
    } catch (error) {
      console.error('Could not save calendar entries:', error)
      setStorageError('Your changes could not be saved. Check your browser storage and try again.')
    }
  }

  const calendarDays = useMemo(() => {
    const firstOfMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), 1)
    const start = new Date(firstOfMonth)
    start.setDate(start.getDate() - firstOfMonth.getDay())
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start)
      date.setDate(start.getDate() + index)
      return date
    })
  }, [visibleMonth])

  const selectedEntries = useMemo(
    () => entries
      .filter((entry) => entry.date === selectedDate)
      .sort((first, second) => (first.time || '').localeCompare(second.time || '')),
    [entries, selectedDate],
  )

  function changeMonth(offset) {
    const nextMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + offset, 1)
    setVisibleMonth(nextMonth)
    const nextSelectedDate = new Date(
      nextMonth.getFullYear(),
      nextMonth.getMonth(),
      Math.min(
        Number(selectedDate.slice(-2)),
        new Date(nextMonth.getFullYear(), nextMonth.getMonth() + 1, 0).getDate(),
      ),
    )
    setSelectedDate(toDateKey(nextSelectedDate))
  }

  function addEntry(event) {
    event.preventDefault()
    const trimmedTitle = title.trim()
    if (!trimmedTitle) return

    const newEntry = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      title: trimmedTitle,
      type,
      date: selectedDate,
      time,
      details: details.trim(),
      completed: false,
    }
    saveEntries([newEntry, ...entries])
    if (storageKey) {
      api.saveCalendarEntry(newEntry)
        .then(() => setSyncError(''))
        .catch((error) => {
          console.error('Could not save calendar entry to the reminder service:', error)
          setSyncError('This entry is saved on this device, but its reminder could not sync.')
        })
    }
    setTitle('')
    setTime('')
    setDetails('')
  }

  async function toggleTask(entryId) {
    const selectedEntry = entries.find((entry) => entry.id === entryId)
    if (storageKey && selectedEntry) {
      try {
        await api.updateCalendarTask(entryId, !selectedEntry.completed)
      } catch (error) {
        console.error('Could not update calendar task:', error)
        setSyncError('The task status could not sync. Please try again.')
        return
      }
    }
    saveEntries(entries.map((entry) => (
      entry.id === entryId ? { ...entry, completed: !entry.completed } : entry
    )))
    setSyncError('')
  }

  async function removeEntry(entryId) {
    if (storageKey) {
      try {
        await api.deleteCalendarEntry(entryId)
      } catch (error) {
        console.error('Could not delete calendar entry from the reminder service:', error)
        setSyncError('The entry could not be deleted from reminders. Please try again.')
        return
      }
    }
    saveEntries(entries.filter((entry) => entry.id !== entryId))
    setSyncError('')
  }

  return (
    <main className="calendar-page">
      <header className="calendar-page-header">
        <div>
          <p className="calendar-eyebrow">PLAN YOUR DAYS</p>
          <h1><CalendarDays size={26} aria-hidden="true" /> Calendar</h1>
          <p>Keep track of reminders, notes, and tasks in one place.</p>
        </div>
      </header>

      <div className="calendar-layout">
        <section className="calendar-month-card" aria-label="Monthly calendar">
          <div className="calendar-month-header">
            <h2>{visibleMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</h2>
            <div className="calendar-month-controls">
              <button type="button" aria-label="Previous month" onClick={() => changeMonth(-1)}>
                <ChevronLeft size={18} />
              </button>
              <button type="button" aria-label="Next month" onClick={() => changeMonth(1)}>
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          <div className="calendar-grid" aria-label="Calendar days">
            {weekdays.map((weekday) => (
              <span className="calendar-weekday" key={weekday}>{weekday}</span>
            ))}
            {calendarDays.map((date) => {
              const dateKey = toDateKey(date)
              const hasEntries = entries.some((entry) => entry.date === dateKey)
              const isCurrentMonth = date.getMonth() === visibleMonth.getMonth()
              return (
                <button
                  className={[
                    'calendar-day',
                    isCurrentMonth ? '' : 'is-outside-month',
                    dateKey === selectedDate ? 'is-selected' : '',
                    dateKey === today ? 'is-today' : '',
                  ].filter(Boolean).join(' ')}
                  type="button"
                  aria-label={`${date.toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' })}${hasEntries ? ', has entries' : ''}`}
                  aria-pressed={dateKey === selectedDate}
                  key={dateKey}
                  onClick={() => {
                    setSelectedDate(dateKey)
                    if (!isCurrentMonth) {
                      setVisibleMonth(new Date(date.getFullYear(), date.getMonth(), 1))
                    }
                  }}
                >
                  <span>{date.getDate()}</span>
                  {hasEntries && <span className="calendar-day-indicator" aria-hidden="true" />}
                </button>
              )
            })}
          </div>

          <div className="calendar-legend">
            <span className="calendar-day-indicator" aria-hidden="true" />
            <span>Days with reminders, notes, or tasks</span>
          </div>
        </section>

        <section className="calendar-agenda">
          <div className="calendar-agenda-header">
            <div>
              <p className="calendar-eyebrow">SELECTED DAY</p>
              <h2>{formatSelectedDate(selectedDate)}</h2>
            </div>
          </div>

          <form className="calendar-entry-form" onSubmit={addEntry}>
            <h3><Plus size={18} aria-hidden="true" /> Add to your calendar</h3>
            <label>
              Title
              <input
                type="text"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="What do you need to remember?"
                maxLength={120}
                required
              />
            </label>
            <div className="calendar-form-row">
              <label>
                Type
                <select value={type} onChange={(event) => setType(event.target.value)}>
                  {entryTypes.map((entryType) => <option key={entryType}>{entryType}</option>)}
                </select>
              </label>
              <label>
                Time <span className="calendar-optional">(optional)</span>
                <input type="time" value={time} onChange={(event) => setTime(event.target.value)} />
              </label>
            </div>
            <label>
              Note <span className="calendar-optional">(optional)</span>
              <textarea
                value={details}
                onChange={(event) => setDetails(event.target.value)}
                placeholder="Add a few details"
                rows="3"
                maxLength={1000}
              />
            </label>
            <button className="calendar-add-button" type="submit">
              <Plus size={16} aria-hidden="true" /> Add {type.toLowerCase()}
            </button>
          </form>

          {storageError && <p className="calendar-storage-error" role="alert">{storageError}</p>}
          {syncError && <p className="calendar-storage-error" role="alert">{syncError}</p>}

          <div className="calendar-entry-list">
            <h3>{selectedEntries.length ? `Your plans (${selectedEntries.length})` : 'Nothing planned yet'}</h3>
            {selectedEntries.length === 0 ? (
              <p className="calendar-empty-state">Add a reminder, note, or task for this day.</p>
            ) : (
              selectedEntries.map((entry) => (
                <article className={`calendar-entry calendar-entry-${entry.type.toLowerCase()}${entry.completed ? ' is-completed' : ''}`} key={entry.id}>
                  {entry.type === 'Task' && (
                    <button
                      className="calendar-task-toggle"
                      type="button"
                      aria-label={entry.completed ? `Mark ${entry.title} incomplete` : `Complete ${entry.title}`}
                      aria-pressed={entry.completed}
                      onClick={() => toggleTask(entry.id)}
                    >
                      {entry.completed && <Check size={14} />}
                    </button>
                  )}
                  <div className="calendar-entry-content">
                    <div className="calendar-entry-title-row">
                      <strong>{entry.title}</strong>
                      <span className="calendar-entry-type">{entry.type}</span>
                    </div>
                    {(entry.time || entry.details) && (
                      <p className="calendar-entry-meta">
                        {entry.time && <time>{entry.time}</time>}
                        {entry.time && entry.details && <span aria-hidden="true"> · </span>}
                        {entry.details}
                      </p>
                    )}
                  </div>
                  <button
                    className="calendar-delete-button"
                    type="button"
                    aria-label={`Delete ${entry.title}`}
                    onClick={() => removeEntry(entry.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                </article>
              ))
            )}
          </div>
        </section>
      </div>
    </main>
  )
}
