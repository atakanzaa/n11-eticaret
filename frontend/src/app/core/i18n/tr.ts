/**
 * Turkish lookup tables for the SmartCommerce UI.
 *
 * Two reasons for keeping this in code (instead of @angular/localize, ngx-translate, etc.):
 *
 * 1. Status enums (OrderStatus, PaymentStatus, ReturnReasonCode, …) come from the
 *    backend as machine codes; we map them to human strings on display.
 * 2. The whole codebase is Turkish-only — a fully-fledged i18n library would
 *    add weight without buying us anything.
 *
 * Strict rule: every Turkish string the user might see goes here. Templates
 * use the `t` pipe; services use `I18nService.t(...)`. No string literals in
 * components or templates.
 */
export const TR = {
  common: {
    loading: 'Yükleniyor…',
    error: 'Bir hata oluştu',
    retry: 'Tekrar Dene',
    empty: 'Sonuç bulunamadı',
    save: 'Kaydet',
    cancel: 'Vazgeç',
    confirm: 'Onayla',
    delete: 'Sil',
    edit: 'Düzenle',
    add: 'Ekle',
    update: 'Güncelle',
    apply: 'Uygula',
    close: 'Kapat',
    next: 'İleri',
    back: 'Geri',
    yes: 'Evet',
    no: 'Hayır',
    optional: 'opsiyonel',
    required: 'zorunlu',
    search: 'Ara',
    filter: 'Filtrele',
    sort: 'Sırala',
    showMore: 'Daha Fazla Göster',
    showLess: 'Daha Az Göster',
    addToCart: 'Sepete Ekle',
    buyNow: 'Hemen Al',
    favorite: 'Favorilere Ekle',
    free: 'Ücretsiz',
    discount: 'İndirim',
    total: 'Toplam',
    subtotal: 'Ara Toplam',
    shipping: 'Kargo',
    vat: 'KDV',
  },

  nav: {
    home: 'Anasayfa',
    categories: 'Kategoriler',
    cart: 'Sepetim',
    account: 'Hesabım',
    orders: 'Siparişlerim',
    profile: 'Profil',
    addresses: 'Adreslerim',
    favorites: 'Favorilerim',
    coupons: 'Kuponlarım',
    returns: 'İade Taleplerim',
    login: 'Giriş Yap',
    register: 'Üye Ol',
    logout: 'Çıkış Yap',
    seller: 'Satıcı Paneli',
    admin: 'Yönetici Paneli',
  },

  auth: {
    loginTitle: 'Hesabınıza giriş yapın',
    registerTitle: 'Yeni hesap oluşturun',
    email: 'E-posta',
    password: 'Şifre',
    passwordAgain: 'Şifre Tekrar',
    firstName: 'Ad',
    lastName: 'Soyad',
    phone: 'Telefon',
    role: 'Rol',
    forgotPassword: 'Şifremi Unuttum',
    haveAccount: 'Zaten üye misiniz?',
    noAccount: 'Henüz üye değil misiniz?',
    loginSubmit: 'Giriş Yap',
    registerSubmit: 'Üye Ol',
    invalidCredentials: 'E-posta veya şifre hatalı',
    sessionExpired: 'Oturumunuz sona erdi, lütfen tekrar giriş yapın',
    unauthorized: 'Bu sayfayı görüntüleme yetkiniz yok',
  },

  product: {
    description: 'Açıklama',
    features: 'Özellikler',
    reviews: 'Değerlendirmeler',
    questions: 'Soru-Cevap',
    shipping: 'Kargo & İade',
    similar: 'Benzer Ürünler',
    inStock: 'Stokta',
    lowStock: 'Son {{count}} adet',
    outOfStock: 'Tükendi',
    freeShipping: 'Ücretsiz Kargo',
    fastShipping: 'Hızlı Teslimat',
    seller: 'Satıcı',
    rating: 'Puan',
    reviewCount: '{{count}} değerlendirme',
    noReviews: 'Henüz değerlendirme yok. İlk siz yazın!',
    addReview: 'Değerlendirme Yaz',
    helpful: 'Faydalı',
    verified: 'Onaylanmış Alıcı',
  },

  cart: {
    title: 'Sepetim',
    empty: 'Sepetiniz boş',
    emptyHint: 'Ürünleri keşfedin ve sepete ekleyin.',
    item: 'ürün',
    items: 'ürün',
    couponCode: 'Kupon Kodu',
    couponApply: 'Uygula',
    couponInvalid: 'Kupon kodu geçerli değil',
    couponApplied: 'Kupon uygulandı: {{discount}} indirim',
    quantity: 'Adet',
    remove: 'Kaldır',
    moveToFavorites: 'Favorilere Taşı',
    proceedCheckout: 'Ödemeye Geç',
    keepShopping: 'Alışverişe Devam Et',
  },

  checkout: {
    title: 'Ödeme',
    stepAddress: 'Teslimat Adresi',
    stepPayment: 'Ödeme Yöntemi',
    stepConfirm: 'Onay',
    addressNew: 'Yeni Adres Ekle',
    addressLabel: 'Adres Başlığı',
    cardHolder: 'Kart Üzerindeki İsim',
    cardNumber: 'Kart Numarası',
    cardExpire: 'Son Kullanma',
    cardCvc: 'CVV',
    installment: 'Taksit',
    installmentSingle: 'Tek Çekim',
    installmentMonths: '{{n}} ay',
    threeDsToggle: '3D Secure ile öde',
    termsAccept: 'Mesafeli satış sözleşmesini okudum, kabul ediyorum',
    placeOrder: 'Siparişi Tamamla',
    fraudCheckPending: 'Doğrulanıyor…',
    fraudCheckPassed: '✓ Güvenli işlem',
    fraudCheckFailed: 'İşlem güvenlik sebebiyle reddedildi',
    paymentChallenge: '3D Secure doğrulama yapılıyor, lütfen bekleyin…',
    paymentSuccess: 'Ödemeniz başarıyla alındı',
    paymentFailure: 'Ödeme başarısız oldu',
    orderNumber: 'Sipariş No',
    estimatedDelivery: 'Tahmini Teslimat',
    backToShopping: 'Alışverişe Devam Et',
  },

  orderStatus: {
    PENDING: 'Onay Bekleniyor',
    CONFIRMED: 'Onaylandı',
    CANCELLED: 'İptal Edildi',
    EXPIRED: 'Süresi Doldu',
  },

  shipmentStatus: {
    PENDING: 'Hazırlanıyor',
    DISPATCHED: 'Kargoya Verildi',
    IN_TRANSIT: 'Yolda',
    DELIVERED: 'Teslim Edildi',
    FAILED: 'Teslim Başarısız',
  },

  paymentStatus: {
    INITIATED: 'Başlatıldı',
    PENDING: 'Beklemede',
    THREEDS_PENDING: '3D Secure Bekleniyor',
    SUCCEEDED: 'Başarılı',
    FAILED: 'Başarısız',
    REFUNDED: 'İade Edildi',
  },

  returnStatus: {
    REQUESTED: 'Talep Edildi',
    APPROVED: 'Onaylandı',
    REJECTED: 'Reddedildi',
    REFUNDED: 'Para İadesi Yapıldı',
    COMPLETED: 'Tamamlandı',
  },

  returnReason: {
    BUYER_REQUEST: 'Vazgeçtim',
    DAMAGED: 'Hasarlı geldi',
    WRONG_ITEM: 'Yanlış ürün geldi',
    NOT_AS_DESCRIBED: 'Açıklamayla uyuşmuyor',
    DEFECTIVE: 'Kusurlu',
    OTHER: 'Diğer',
  },

  fraudDecision: {
    APPROVE: 'Onaylandı',
    REVIEW: 'İncelemede',
    BLOCK: 'Reddedildi',
  },

  blacklistType: {
    USER_ID: 'Kullanıcı',
    EMAIL: 'E-posta',
    IP: 'IP Adresi',
    CARD_BIN: 'Kart BIN',
  },

  notificationStatus: {
    PENDING: 'Beklemede',
    SENT: 'Gönderildi',
    FAILED: 'Başarısız',
  },

  notificationChannel: {
    EMAIL: 'E-posta',
    SMS: 'SMS',
    PUSH: 'Bildirim',
  },

  account: {
    title: 'Hesabım',
    profileUpdated: 'Profil güncellendi',
    addressAdded: 'Adres eklendi',
    addressUpdated: 'Adres güncellendi',
    addressDeleted: 'Adres silindi',
    kvkkConsent: 'KVKK aydınlatma metnini okudum, kabul ediyorum',
    deleteAccount: 'Hesabımı Sil',
    deleteAccountConfirm: 'Hesabınızı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.',
  },

  seller: {
    dashboard: 'Anasayfa',
    products: 'Ürünlerim',
    orders: 'Siparişlerim',
    stock: 'Stok Yönetimi',
    returns: 'İade Talepleri',
    promotions: 'Kampanyalar',
    reports: 'Raporlar',
    settings: 'Mağaza Ayarları',
    todayRevenue: "Bugünün Cirosu",
    pendingOrders: "Bekleyen Sipariş",
    lowStock: "Stoğu Azalan Ürün",
    rating: "Müşteri Puanı",
    last30DaysRevenue: 'Son 30 Gün Cirosu',
    addProduct: 'Yeni Ürün Ekle',
    updateStock: 'Stok Güncelle',
  },

  admin: {
    overview: 'Genel Bakış',
    sellers: 'Satıcılar',
    products: 'Ürünler',
    promotions: 'Promosyonlar',
    fraud: 'Fraud',
    aiUsage: 'AI Kullanımı',
    systemHealth: 'Sistem Sağlığı',
    gmv30d: 'Son 30 Gün GMV',
    activeSellers: 'Aktif Satıcı',
    orders30d: 'Son 30 Gün Sipariş',
    avgBasket: 'Ortalama Sepet',
    fraudRejectionRate: 'Fraud Red Oranı',
  },

  fraud: {
    riskScore: 'Risk Skoru',
    triggeredRules: 'Tetiklenen Kurallar',
    blacklist: 'Kara Liste',
    addToBlacklist: 'Kara Listeye Ekle',
    removeFromBlacklist: 'Kara Listeden Çıkar',
  },

  ai: {
    assistant: 'Alışveriş Asistanı',
    placeholder: 'Bir şey yazın…',
    welcome: 'Merhaba! Size nasıl yardımcı olabilirim?',
    todayUsed: 'Bugün Harcanan',
    dailyBudget: 'Günlük Bütçe',
    remaining: 'Kalan',
    providerDistribution: 'Sağlayıcı Dağılımı',
    topTools: 'En Çok Çağrılan Araçlar',
    dailySpend: 'Günlük Harcama',
  },

  validation: {
    required: 'Bu alan zorunludur',
    email: 'Geçerli bir e-posta giriniz',
    minLength: 'En az {{n}} karakter olmalı',
    maxLength: 'En fazla {{n}} karakter olabilir',
    min: 'En az {{n}} olmalı',
    max: 'En fazla {{n}} olabilir',
    cardNumber: 'Geçerli bir kart numarası giriniz',
    cardExpire: 'Geçerli bir tarih giriniz',
    cardCvc: '3 veya 4 haneli güvenlik kodu',
  },
} as const;

export type I18nDictionary = typeof TR;
