package cn.edu.hut.wx.controller;

import cn.hutool.Hutool;
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
}
