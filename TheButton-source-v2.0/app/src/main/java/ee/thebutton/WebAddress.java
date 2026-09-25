package ee.thebutton;

import java.net.URI;
import java.util.Locale;

/** Independent of Android so the URL boundary can be tested on the JVM. */
final class WebAddress {
    static String normalize(String input) {
        String value=input==null?"":input.trim();
        if(value.isEmpty())throw new IllegalArgumentException("Kleebi kõigepealt veebilehe link.");
        if(!value.matches("(?i)^[a-z][a-z0-9+.-]*:.*"))value="https://"+value;
        try {
            URI uri=new URI(value);
            String scheme=uri.getScheme().toLowerCase(Locale.ROOT);
            if(!(scheme.equals("https")||scheme.equals("http"))||uri.getHost()==null||uri.getHost().isEmpty()
                ||uri.getRawUserInfo()!=null||uri.getPort()>65535||uri.getPort()==0)
                throw new IllegalArgumentException("Kasuta terviklikku http:// või https:// veebilehe linki.");
            return scheme+value.substring(value.indexOf(':'));
        } catch(java.net.URISyntaxException e) { throw new IllegalArgumentException("Link ei ole korrektne. Kontrolli aadressi ja tühikuid."); }
    }
    static boolean allowed(String value) {
        if(value==null||!(value.regionMatches(true,0,"https://",0,8)||value.regionMatches(true,0,"http://",0,7)))return false;
        try { normalize(value);return true; } catch(IllegalArgumentException e) { return false; }
    }
}
