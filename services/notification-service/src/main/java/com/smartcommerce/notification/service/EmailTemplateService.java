package com.smartcommerce.notification.service;

import com.smartcommerce.notification.api.dto.EmailJob;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {
    public String renderBody(EmailJob job) {
        var model = job.model();
        return switch (job.templateCode()) {
            case "WELCOME" -> "Merhaba " + model.getOrDefault("name", "") + ", SmartCommerce'a hos geldin.";
            case "ORDER_CONFIRMED" -> "Siparisiniz onaylandi. Siparis no: " + model.getOrDefault("orderNumber", "-")
                + ", toplam: " + model.getOrDefault("grandTotal", "-") + " " + model.getOrDefault("currency", "TRY");
            case "ORDER_CANCELLED" -> "Siparisiniz iptal edildi. Siparis no: " + model.getOrDefault("orderNumber", "-")
                + ", neden: " + model.getOrDefault("reason", "-");
            case "ORDER_RECEIVED" -> "Merhaba " + model.getOrDefault("name", "") + ", siparişiniz "
                + model.getOrDefault("orderNumber", "-") + " teslim alındı olarak işaretlendi. "
                + "Aldığınız ürünleri değerlendirerek diğer alıcılara yardımcı olabilirsiniz.";
            case "REVIEW_APPROVED" -> "Merhaba " + model.getOrDefault("name", "") + ", "
                + model.getOrDefault("rating", "-") + " yıldızlı yorumunuz yayınlandı: \""
                + model.getOrDefault("title", "") + "\". Teşekkürler!";
            case "ADMIN_ALERT" -> "[ALERT] " + model.getOrDefault("title", "") + " — "
                + model.toString();
            default -> String.valueOf(model);
        };
    }
}
