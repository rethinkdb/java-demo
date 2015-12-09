var app = new Vue({
  el: "body",
  data: {
    identified: false,
    username: null,
    message: null,
    messages: []
  },
  methods: {
    connect: function(messages) {
      this.messages = messages;
      setTimeout(this.scrollBottom);
      var bus = new vertx.EventBus("/eventbus");
      bus.onopen = () => bus.registerHandler("chat", this.onMessage);
    },
    scrollBottom: function() {
      var el = document.getElementById("messages");
      el.scrollTop = el.scrollHeight;
    },
    onMessage: function(message) {
      this.messages.push(message);
      setTimeout(this.scrollBottom);
    },
    sendMessage: function() {
      fetch("/send", {
        method: "post",
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          user: this.username,
          text: this.message
        })
      });

      this.message = null;
    },
    login: function(event) {
      this.identified = true;
      fetch("/messages").then(res => res.json()).then(this.connect);
    }
  }
});
