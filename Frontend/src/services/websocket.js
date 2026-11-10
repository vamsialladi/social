import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.subscriptions = new Map()
    this.statusListeners = new Set()
    this.messageListeners = new Set()
    this.recentMessageIds = new Set()
    this.currentEmail = ''
    this.currentToken = ''
  }

  connect(token, userEmail) {
    if (this.client && this.connected && this.currentEmail === userEmail) {
      return
    }

    if (this.client) {
      try {
        this.client.deactivate()
      } catch (err) {
        // ignore
      }
    }

    this.currentToken = token || localStorage.getItem('truehunt_token') || ''
    let parsedUser = null
    try {
      const u = localStorage.getItem('truehunt_user')
      if (u) parsedUser = JSON.parse(u)
    } catch {}

    this.currentEmail = (userEmail || parsedUser?.email || 'pro@truehunt.in').trim()
    this.currentUserId = String(parsedUser?.userId || parsedUser?.publicUserId || parsedUser?.id || '').trim()
    this.currentUserName = String(parsedUser?.name || '').trim()

    const headers = {
      ...(this.currentToken ? { Authorization: `Bearer ${this.currentToken}` } : {}),
      ...(this.currentEmail ? { 'X-User-Email': this.currentEmail } : {}),
      ...(this.currentUserId ? { 'X-User-Id': this.currentUserId } : {}),
      ...(this.currentUserName ? { 'X-User-Name': this.currentUserName } : {}),
      user: this.currentEmail,
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: headers,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (str) => {
        // console.debug('[STOMP]', str)
      },
      onConnect: (frame) => {
        this.connected = true
        this.notifyStatus(true)

        // 1. Subscribe to user-specific private queue (/user/queue/messages)
        this.doSubscribe('/user/queue/messages', (msg) => {
          this.notifyMessage(msg)
        })

        // 2. Global message streams for instant real-time delivery
        this.doSubscribe('/topic/messages', (msg) => {
          this.notifyMessage(msg)
        })
        this.doSubscribe('/topic/chat', (msg) => {
          this.notifyMessage(msg)
        })

        // 3. Real-time connections topics (instant request & accept notifications)
        this.doSubscribe('/topic/connections', (event) => {
          try {
            window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
              detail: event,
            }))
          } catch (e) {}
        })

        // 4. Direct pub-sub topics for user's email, ID, and name for guaranteed realtime dispatch
        if (this.currentEmail) {
          const emailLower = this.currentEmail.toLowerCase()
          this.doSubscribe(`/topic/user.${emailLower}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
          this.doSubscribe(`/topic/user.${emailLower}.connections`, (event) => {
            try {
              window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
                detail: event,
              }))
            } catch (e) {}
          })
          if (emailLower !== this.currentEmail) {
            this.doSubscribe(`/topic/user.${this.currentEmail}.messages`, (msg) => {
              this.notifyMessage(msg)
            })
            this.doSubscribe(`/topic/user.${this.currentEmail}.connections`, (event) => {
              try {
                window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
                  detail: event,
                }))
              } catch (e) {}
            })
          }
        }
        if (this.currentUserId) {
          this.doSubscribe(`/topic/user.${this.currentUserId}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
          this.doSubscribe(`/topic/user.${this.currentUserId.toLowerCase()}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
          this.doSubscribe(`/topic/user.${this.currentUserId.toLowerCase()}.connections`, (event) => {
            try {
              window.dispatchEvent(new CustomEvent('truehunt-connection-changed', {
                detail: event,
              }))
            } catch (e) {}
          })
        }
        if (this.currentUserName) {
          this.doSubscribe(`/topic/user.${this.currentUserName}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
          const nameNoSpaces = this.currentUserName.toLowerCase().replace(/\s+/g, '')
          this.doSubscribe(`/topic/user.${nameNoSpaces}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
        }

        // 5. Resubscribe any registered destinations
        for (const [dest, callback] of this.subscriptions.entries()) {
          this.doSubscribe(dest, callback)
        }
      },
      onDisconnect: () => {
        this.connected = false
        this.notifyStatus(false)
      },
      onStompError: (frame) => {
        console.warn('[STOMP Error]', frame.headers['message'], frame.body)
      },
      onWebSocketClose: () => {
        this.connected = false
        this.notifyStatus(false)
      },
    })

    this.client.activate()
  }

  subscribe(destination, callback) {
    this.subscriptions.set(destination, callback)

    let sub = null
    if (this.connected && this.client) {
      sub = this.doSubscribe(destination, callback)
    }

    return () => {
      this.subscriptions.delete(destination)
      if (sub) {
        try {
          sub.unsubscribe()
        } catch (e) {
          // ignore
        }
      }
    }
  }

  doSubscribe(destination, callback) {
    if (!this.client || !this.connected) return null
    try {
      return this.client.subscribe(destination, (message) => {
        try {
          const payload = JSON.parse(message.body)
          callback(payload)
        } catch {
          callback(message.body)
        }
      })
    } catch (e) {
      console.warn('[STOMP] Subscribe error for', destination, e)
      return null
    }
  }

  sendMessage(destination, payload) {
    if (!this.client || !this.connected) {
      console.warn('[STOMP] Client not connected. Cannot send to', destination)
      return false
    }

    try {
      this.client.publish({
        destination,
        body: JSON.stringify(payload),
        headers: {
          ...(this.currentEmail ? { 'X-User-Email': this.currentEmail } : {}),
        },
      })
      return true
    } catch (e) {
      console.error('[STOMP] Publish error:', e)
      return false
    }
  }

  onStatusChange(listener) {
    this.statusListeners.add(listener)
    listener(this.connected)
    return () => this.statusListeners.delete(listener)
  }

  onMessage(listener) {
    this.messageListeners.add(listener)
    return () => this.messageListeners.delete(listener)
  }

  notifyStatus(status) {
    this.statusListeners.forEach((fn) => {
      try { fn(status) } catch (e) { console.error(e) }
    })
  }

  notifyMessage(message) {
    if (!message) return
    const dedupKey = message.id
      ? String(message.id)
      : `${message.senderId}_${message.content}_${message.sentAt || ''}`

    if (this.recentMessageIds.has(dedupKey)) {
      return
    }
    this.recentMessageIds.add(dedupKey)
    setTimeout(() => {
      this.recentMessageIds.delete(dedupKey)
    }, 5000)

    this.messageListeners.forEach((fn) => {
      try { fn(message) } catch (e) { console.error(e) }
    })
    try {
      window.dispatchEvent(new CustomEvent('truehunt-message-received', { detail: message }))
    } catch (e) {}
  }

  disconnect() {
    if (this.client) {
      try {
        this.client.deactivate()
      } catch (e) {
        // ignore
      }
      this.client = null
    }
    this.connected = false
    this.notifyStatus(false)
  }
}

export const websocketService = new WebSocketService()
export default websocketService

