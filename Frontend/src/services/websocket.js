import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.subscriptions = new Map()
    this.statusListeners = new Set()
    this.messageListeners = new Set()
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

    this.currentEmail = userEmail || parsedUser?.email || 'pro@truehunt.in'
    this.currentUserId = parsedUser?.userId || parsedUser?.publicUserId || parsedUser?.id || ''

    const headers = {
      ...(this.currentToken ? { Authorization: `Bearer ${this.currentToken}` } : {}),
      ...(this.currentEmail ? { 'X-User-Email': this.currentEmail } : {}),
      ...(this.currentUserId ? { 'X-User-Id': String(this.currentUserId) } : {}),
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

        // 2. Direct pub-sub topics for user's email & ID for guaranteed realtime dispatch
        if (this.currentEmail) {
          this.doSubscribe(`/topic/user.${this.currentEmail.toLowerCase()}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
        }
        if (this.currentUserId) {
          this.doSubscribe(`/topic/user.${this.currentUserId}.messages`, (msg) => {
            this.notifyMessage(msg)
          })
        }

        // 3. Resubscribe any registered destinations
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
    this.messageListeners.forEach((fn) => {
      try { fn(message) } catch (e) { console.error(e) }
    })
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

