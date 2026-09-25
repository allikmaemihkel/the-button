package ee.thebutton;

public final class WebAddressTest {
    private static int count;
    private static void normalized(String input,String expected) {
        if(!WebAddress.normalize(input).equals(expected))throw new AssertionError(input);count++;
    }
    private static void rejected(String input) {
        try { WebAddress.normalize(input); } catch(IllegalArgumentException expected) {count++;return;}
        throw new AssertionError("Accepted unsafe/invalid input: "+input);
    }
    public static void main(String[] args) {
        normalized("example.com/page?q=hello#section","https://example.com/page?q=hello#section");
        normalized("  https://example.com  ","https://example.com");
        normalized("HTTPS://example.com/path","https://example.com/path");
        normalized("http://192.168.1.2:8080/dashboard","http://192.168.1.2:8080/dashboard");
        normalized("https://example.com/?redirect=https%3A%2F%2Fother.example","https://example.com/?redirect=https%3A%2F%2Fother.example");
        for(String value:new String[]{null,""," ","javascript:alert(1)","data:text/html,test","file:///etc/passwd","content://provider/file",
                "intent://test","ftp://example.com","https://user:pass@example.com","https:///broken","https://example.com/a b",
                "https://example.com:99999","https://example.com:0","https://example.com\n/path"})rejected(value);
        if(WebAddress.allowed("example.com")||WebAddress.allowed("//example.com")||WebAddress.allowed("javascript:alert(1)"))throw new AssertionError("Navigation must have an explicit HTTP(S) scheme");
        if(!WebAddress.allowed("https://example.com/next")||!WebAddress.allowed("http://example.com/next"))throw new AssertionError("Blocked normal navigation");
        System.out.println("PASS: "+count+" normalization/rejection cases and navigation checks");
    }
}
