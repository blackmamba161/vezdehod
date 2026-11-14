package local.blackmamba.vpn;
/** Public resolver endpoints verified against provider documentation. Filtering is always explicit. */
final class DnsProviders {
 static final class Provider {final String name,address;final int detail;Provider(String name,String address,int detail){this.name=name;this.address=address;this.detail=detail;}}
 static final Provider[] ALL={
  new Provider("Google",DnsSettings.GOOGLE,R.string.dns_unfiltered),
  new Provider("Cloudflare",DnsSettings.CLOUDFLARE,R.string.dns_unfiltered),
  new Provider("Quad9","https://dns10.quad9.net/dns-query",R.string.dns_unfiltered),
  new Provider("AdGuard","https://unfiltered.adguard-dns.com/dns-query",R.string.dns_unfiltered),
  new Provider("Control D","https://freedns.controld.com/p0",R.string.dns_unfiltered),
  new Provider("Yandex","tls://common.dot.dns.yandex.net",R.string.dns_unfiltered_tls),
  new Provider("Quad9","https://dns.quad9.net/dns-query",R.string.dns_threats),
  new Provider("AdGuard","https://dns.adguard-dns.com/dns-query",R.string.dns_ads)
 };
 static Provider find(String address){if(address.equals("https://1.1.1.1/dns-query"))return ALL[1];for(Provider p:ALL)if(p.address.equals(address))return p;return null;}
}
