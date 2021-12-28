# Tiny Lynx IntelliJ Plugin

The plugin provides the integration of third-party spellcheck (proofreading) tools in [IntelliJ Platform](https://www.jetbrains.com/idea/) IDEs.

### Features:

- On-the-fly proofreading (sends requests via the web client end renders warnings/suggestions as you type). Note: warning highlights appear after a delay due to the network lag. The delay depends on the network connection. A user can turn off the on-the-fly mode.

- Proofreading during the code analysis. Note: code analysis takes longer than usual because network requests are sent synchronously.

- Caching results to speed up checks after the initial one. The lifespan of the cache is a user setting.
  
- Ability to run an arbitrary check in a separate tool window (see "Tiny Lynx Proofreading > Open Tool Window" in the editor's context menu).

- Display of the complete diagnostic information coming from a 3rd-party service.

### Requirements:

IntelliJ IDE (the `2020.1` generation or newer).

You can use the plugin together with the built-int Grazie solution (if you do so, it's better to disable the "on the fly" feature of Tiny Lynx). Or else, you can disable the Grazie plugin and use Tiny Lynx for
both on the fly and dedicated proofreading.

### Third-party integration:

Currently, the project integrates the [Grammarly](https://www.grammarly.com/) free online writing assistant. The websocket-based client is inspired by [this GitHub project](https://github.com/stewartmcgown/grammarly-api).

There are more client integrations to come.