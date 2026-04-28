package com.smartcommerce.returns.domain;

public enum ReturnReason {
    DEFECTIVE("Ürün arızalı"),
    NOT_AS_DESCRIBED("Açıklamaya uygun değil"),
    WRONG_ITEM("Yanlış ürün gönderildi"),
    DAMAGED_IN_SHIPPING("Kargoda hasar gördü"),
    CHANGED_MIND("Vazgeçtim"),
    SIZE_FIT("Beden/uyum problemi"),
    OTHER("Diğer");

    private final String description;

    ReturnReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
