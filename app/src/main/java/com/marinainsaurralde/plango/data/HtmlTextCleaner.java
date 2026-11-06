package com.marinainsaurralde.plango.data;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlTextCleaner {
    private static final Pattern ENTITY = Pattern.compile("&(#(?:x[0-9a-fA-F]+|[0-9]+)|[a-zA-Z]+);");
    private static final Map<String, String> NAMED = new HashMap<>();
    static {
        String[][] values={{"amp","&"},{"lt","<"},{"gt",">"},{"quot","\""},{"apos","'"},{"nbsp"," "},
                {"aacute","á"},{"eacute","é"},{"iacute","í"},{"oacute","ó"},{"uacute","ú"},{"ntilde","ñ"},
                {"Aacute","Á"},{"Eacute","É"},{"Iacute","Í"},{"Oacute","Ó"},{"Uacute","Ú"},{"Ntilde","Ñ"},
                {"uuml","ü"},{"Uuml","Ü"},{"ndash","–"},{"mdash","—"},{"hellip","…"},{"middot","·"},
                {"laquo","«"},{"raquo","»"},{"ldquo","“"},{"rdquo","”"},{"lsquo","‘"},{"rsquo","’"},
                {"euro","€"},{"copy","©"},{"reg","®"},{"ordm","º"},{"ordf","ª"},{"ccedil","ç"},
                {"aelig","æ"},{"acute","´"},{"ucirc","û"}};
        for(String[] value:values) NAMED.put(value[0],value[1]);
    }
    private HtmlTextCleaner() { }
    public static String clean(String value) {
        if(value==null)return null;
        String text=value.replaceAll("(?i)<br\\s*/?>|</p>|</li>|</div>|</h[1-6]>","\n")
                .replaceAll("(?s)<[^>]+>"," ");
        for(int pass=0;pass<2;pass++) text=decodeEntities(text);
        text=text.replace('\u00a0',' ').replaceAll("[ \\t\\x0B\\f\\r]+"," ")
                .replaceAll(" *\\n *","\n").replaceAll("\\n{3,}","\n\n").trim();
        return text.isEmpty()?null:text;
    }
    private static String decodeEntities(String value){Matcher matcher=ENTITY.matcher(value);StringBuffer result=new StringBuffer();while(matcher.find()){String token=matcher.group(1),replacement=null;if(token.startsWith("#")){try{int radix=10,start=1;if(token.length()>2&&(token.charAt(1)=='x'||token.charAt(1)=='X')){radix=16;start=2;}replacement=new String(Character.toChars(Integer.parseInt(token.substring(start),radix)));}catch(IllegalArgumentException ignored){}}else replacement=NAMED.get(token);if(replacement==null)replacement=matcher.group();matcher.appendReplacement(result,Matcher.quoteReplacement(replacement));}matcher.appendTail(result);return result.toString();}
}
