# ByeBox implementation gaps against Hiddify

Local Hiddify reference sources are stored at `reference/hiddify-app/` and are intentionally ignored by git.

## Claimed but incomplete features

- VPN core lifecycle: current service starts sing-box directly, but still lacks Hiddify's fuller platform integration around service commands, config validation, runtime state, and system proxy handling.
- Routing profile "Bypass LAN, China and Russia": previously only bypassed LAN/private IP ranges. CN/RU domain suffixes are now covered as a first step, but proper geoip/geosite rule-set assets still need to be ported.
- Ad blocking profile: previously rejected only two suffixes. The local list is now wider, but this still needs Hiddify-style remote/local rule-set support.
- DNS "System DNS": previously silently used Cloudflare through proxy. It now maps to sing-box local DNS; next step is Hiddify-like DNS strategy and leak-safe presets.
- IPv6 switch: UI exposed it, while the Android service forces IPv4-only for stability. The switch is now disabled in UI until the TUN/route path is properly implemented.
- Autostart on boot: receiver existed, but there was no visible setting. A visible toggle has been added.
- Quick Settings tile: used stale IPv6 defaults and broken Russian subtitles. Defaults and labels are fixed.
- Android VPN bypass: currently only calls the system bypass API. Missing Hiddify-level per-app include/exclude and package rules.
- Best server / ping all: current checks are TCP latency to host:port, not full proxy health checks through the outbound. Needs Hiddify-style URL test and sortable subscription node list.
- Traffic accounting: notification/UI now use Clash API and device fallbacks, but not Hiddify's complete core stats model.
- Share config: exported links do not preserve every protocol transport parameter yet, especially advanced Reality, WebSocket, HTTP/2, gRPC, TUIC, Hysteria2 and WireGuard fields.

## Transfer order

1. Stabilize Android VPN service and sing-box config generation.
2. Port Hiddify-like routing/DNS/rule-set model.
3. Port full proxy health checks, subscription sorting and grouped source operations.
4. Port per-app Android VPN routing.
5. Clean UI claims so every visible setting has a real backend effect.
