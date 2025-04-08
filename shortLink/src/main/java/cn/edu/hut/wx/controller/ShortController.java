package cn.edu.hut.wx.controller;

import cn.hutool.Hutool;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.MD5;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static javax.swing.text.html.HTML.Tag.BASE;

@RestController
@RequestMapping("/shortController")
public class ShortController {

    private static final String SHORT_URL_PREFIX = "http://localhost:8080/shortController/";
    private static final String CHAR_SET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_URL_LENGTH = 6;
    private static final int BASE = CHAR_SET.length();
    private final Map<String, String> shortToLongMap = new HashMap<>();
    private final Map<String, String> longToShortMap = new HashMap<>();
    private final Random random = new Random();

    @PostMapping("/generate")
    public String generateShortUrl(@RequestBody String longUrl) {
        if (longToShortMap.containsKey(longUrl)) {
            return longToShortMap.get(longUrl);
        }
        String shortUrlId;
        do {
            shortUrlId = generateRandomId();
        } while (shortToLongMap.containsKey(shortUrlId));

        shortToLongMap.put(shortUrlId, longUrl);
        longToShortMap.put(longUrl, SHORT_URL_PREFIX + shortUrlId);
        return SHORT_URL_PREFIX + shortUrlId;
    }

    @GetMapping("/{shortUrlId}")
    public RedirectView getLongUrl(@PathVariable String shortUrlId) {
        String longUrl = shortToLongMap.get(shortUrlId);
        if (longUrl != null) {
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(longUrl);
            return redirectView;
        }
        return null;
    }

    //简单短链接生成器
    private String generateRandomId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SHORT_URL_LENGTH; i++) {
            sb.append(CHAR_SET.charAt(random.nextInt(CHAR_SET.length())));
        }
        return sb.toString();
    }
    //MD5短链生成器
    private String generateShortUrlIdByMD5(String longUrl) {
        MD5 md5 = new MD5();
        String md5Hash = md5.digestHex(longUrl);
        // 截取 MD5 哈希值的一部分
        String subHash = md5Hash.substring(0, 8);
        long num = Long.parseLong(subHash, 16);
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHAR_SET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        // 不足指定长度时补零
        while (sb.length() < SHORT_URL_LENGTH) {
            sb.insert(0, CHAR_SET.charAt(0));
        }
        return sb.toString();
    }


    /**
     * 正规原始模板
     */
    class shoryLink{
        public static final String[] chars = new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","0",
                "1","2","3","4","5","6","7","8","9","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};


        /**
         * 一个长链接URL转换为4个短KEY
         * @param url
         * @return
         */
        public static String[] shortUrl(String url){
            String key = "";
            // 对地址进行md5
            String sMD5EncryptResult = DigestUtil.md5Hex(key+url);
            System.out.println("sMD5EncryptResult  " + sMD5EncryptResult);
            String hex = sMD5EncryptResult;
            String[] resUrl = new String[4];
            for (int i = 0; i < 4; i++) {
                // 取出8位字符串,md5 32位,被切割为4组，每组8个字符
                String sTempSubString = hex.substring(i*8,i*8+8);

                // 先转换为16进制，然后用0x3FFFFFFF 进行位与运算，目的是格式化截取前30位
                long lHexLong = 0x3FFFFFFF & Long.parseLong(sTempSubString,16);
                System.out.println("sTempSubString  " + sTempSubString);
                System.out.println("16pare==> "+Long.parseLong(sTempSubString,16));
                System.out.println("lHexLong ==> " + lHexLong);
                String outChars = "";
                System.out.println("-----------------------------------------------");
                for (int j = 0; j < 6; j++) {
                    // 0x0000003D 代表什么意思？他的10进制是61，61代表chars数组长度62的0到61坐标
                    // 0x0000003D & lHexLong 进行位于运算，就是格式化为6位，即61内的数字
                    // 保证了index绝对是61以内的值
                    long index = 0x0000003D & lHexLong;

                    outChars += chars[(int) index];
                    System.out.println(outChars);
                    // 每次循环位移5位，因为30位的二进制，分六次循环，即每次右移5位
                    lHexLong = lHexLong >> 5;
                }
                System.out.println("resUrl="+resUrl.toString());
                // 把字符串存入对应索引的输出数组
                resUrl[i] = outChars;
            }
            return resUrl;
        }

        //    http://huaxin.wkingj.cn/u2A7Zn
        public static void main(String[] args) {
            String longUrl = "http://a.baidu.cn/questionnaire/loadSurvey.html?Mjk0NzI4NzI=&MjAyMeeUqOawtOa7oeaEj+W6puiwg+afpemXruWNt+eAmuiTnemhuuaOp+eJiA==#";
            String[] shortCodeArry = shortUrl(longUrl);

            System.out.println("-----------------------------------------------");
            for (int i = 0; i < shortCodeArry.length; i++) {
                System.out.println(shortCodeArry[i]);
            }
        }
    }
}
