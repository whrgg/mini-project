package cn.edu.hut.wx.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/shortController")
public class ShortController {

    private static final String SHORT_URL_PREFIX = "http://localhost:8080/shortController/";
    private static final String CHAR_SET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_URL_LENGTH = 6;
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
}
