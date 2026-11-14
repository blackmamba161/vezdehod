# DNS policy

The DNS control is under Settings → Connection. Automatic mode is the default. Manual primary, backup, direct and bootstrap values are retained when automatic mode is selected.

Automatic mode uses supported unscoped subscription resolvers, respecting an explicit default resolver tag. Otherwise it uses Google DoH with Cloudflare DoH as backup. Provider-specific scoped DNS remains in preserved Xray graphs; unsupported provider resolver semantics are not flattened into a global resolver.

App and domain rules send DNS through the same selected or explicitly assigned VPN route. Direct rules use the physical network's DNS with a public fallback in automatic mode. The proxy server's own address is resolved separately through the physical network, avoiding a recursive dependency on the tunnel. Manual direct/bootstrap overrides are respected. No VPN DNS failure falls back to a direct route.

The fallback transport bounds each resolver attempt to three seconds. Failure, timeout, REFUSED or SERVFAIL tries the configured backup. Valid NXDOMAIN replies do not trigger fallback. A failed primary has a 30-second cooldown; one subsequent user query tests recovery. There is no periodic recovery worker. Network reset or server selection invalidates outstanding old-network replies and DNS cache. Subscription-specific resolvers follow the active selector, including reserve-server selection, without restarting the VPN.

The Check action starts an isolated engine without a TUN interface, tests VPN and direct DNS routes, and closes the engine, helper processes and temporary cache. Only one DNS check can run at a time. This checks name resolution, not video playback or overall VPN stability.

Verification includes native race-enabled tests for failure, cancellation, cooldown, recovery, concurrent recovery and network/selector changes; policy/import tests; native configuration startup; and an Android check with an unreachable primary and working backup.
