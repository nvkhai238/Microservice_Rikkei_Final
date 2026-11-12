package com.ecommerce.notification.template;

import com.ecommerce.notification.config.NotificationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template định danh theo loại thông báo + kênh + ngôn ngữ + phiên bản (FR-NOT-003):
 * {@code templates/<channel>/<locale>/<code>.v<version>.html}. Dòng đầu là {@code <!-- subject: ... -->}.
 * <ul>
 *   <li>{@code {{name}}}: giá trị được HTML-escape (chống chèn HTML/script vào mail - DR-010).</li>
 *   <li>{@code {{{name}}}}: chèn nguyên văn, CHỈ dùng cho đoạn HTML do code tự dựng và đã escape từng phần.</li>
 * </ul>
 */
@Component
public class TemplateRenderer {
    private static final Pattern RAW = Pattern.compile("\\{\\{\\{(\\w+)}}}");
    private static final Pattern ESCAPED = Pattern.compile("\\{\\{(\\w+)}}");
    private static final Pattern SUBJECT = Pattern.compile("^<!--\\s*subject:\\s*(.+?)\\s*-->\\s*\\R");

    private final NotificationProperties props;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public TemplateRenderer(NotificationProperties props) {
        this.props = props;
    }

    public record Rendered(String code, int version, String locale, String subject, String body) {
    }

    public Rendered render(String channel, String code, Map<String, String> vars) {
        String locale = props.locale();
        int version = props.templateVersion();
        String path = "templates/" + channel.toLowerCase() + "/" + locale + "/" + code + ".v" + version + ".html";
        String source = cache.computeIfAbsent(path, TemplateRenderer::load);
        Matcher sm = SUBJECT.matcher(source);
        String subject = sm.find() ? sm.group(1) : code;
        String body = sm.replaceFirst("");
        return new Rendered(code, version, locale, fill(subject, vars), fill(body, vars));
    }

    private static String fill(String text, Map<String, String> vars) {
        String withRaw = RAW.matcher(text).replaceAll(m -> Matcher.quoteReplacement(vars.getOrDefault(m.group(1), "")));
        return ESCAPED.matcher(withRaw).replaceAll(m ->
                Matcher.quoteReplacement(HtmlUtils.htmlEscape(vars.getOrDefault(m.group(1), ""))));
    }

    private static String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Template not found: " + path, e);
        }
    }
}
