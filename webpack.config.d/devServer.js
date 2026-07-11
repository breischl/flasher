// Make the Kotlin/JS webpack dev server reachable over the LAN (e.g. from a phone),
// not just from localhost. Applied only to the development run task.
config.devServer = config.devServer || {};
config.devServer.host = "0.0.0.0";
// Accept requests whose Host header is a LAN IP instead of localhost.
config.devServer.allowedHosts = "all";
