-- Seeds the eight top-level marketplace categories and a starter brand list.
-- Categories are top-level (parent_id NULL); subcategories can be added later.
-- Slugs match what the Angular header navigation expects.

INSERT INTO categories (id, parent_id, name, slug, description, image_url, display_order, is_active) VALUES
    (uuid_generate_v4(), NULL, 'Elektronik',       'elektronik',       'Telefon, bilgisayar, tablet, ses ve görüntü', NULL, 1, TRUE),
    (uuid_generate_v4(), NULL, 'Moda',             'moda',             'Kadın, erkek ve çocuk giyim, ayakkabı, çanta', NULL, 2, TRUE),
    (uuid_generate_v4(), NULL, 'Kozmetik',         'kozmetik',         'Cilt bakımı, makyaj, parfüm, kişisel bakım', NULL, 3, TRUE),
    (uuid_generate_v4(), NULL, 'Ev & Yaşam',       'ev-yasam',         'Mobilya, dekorasyon, mutfak, yatak ve banyo', NULL, 4, TRUE),
    (uuid_generate_v4(), NULL, 'Süpermarket',      'supermarket',      'Gıda, içecek, temizlik, kişisel bakım', NULL, 5, TRUE),
    (uuid_generate_v4(), NULL, 'Spor & Outdoor',   'spor-outdoor',     'Spor giyim, ayakkabı, ekipman, outdoor', NULL, 6, TRUE),
    (uuid_generate_v4(), NULL, 'Kitap',            'kitap',            'Edebiyat, kişisel gelişim, çocuk kitapları', NULL, 7, TRUE),
    (uuid_generate_v4(), NULL, 'Anne & Bebek',     'anne-bebek',       'Bebek bakımı, oyuncak, çocuk giyim', NULL, 8, TRUE);

INSERT INTO brands (id, name, slug, logo_url, is_active) VALUES
    (uuid_generate_v4(), 'Apple',     'apple',     NULL, TRUE),
    (uuid_generate_v4(), 'Samsung',   'samsung',   NULL, TRUE),
    (uuid_generate_v4(), 'Xiaomi',    'xiaomi',    NULL, TRUE),
    (uuid_generate_v4(), 'Sony',      'sony',      NULL, TRUE),
    (uuid_generate_v4(), 'LG',        'lg',        NULL, TRUE),
    (uuid_generate_v4(), 'Nike',      'nike',      NULL, TRUE),
    (uuid_generate_v4(), 'Adidas',    'adidas',    NULL, TRUE),
    (uuid_generate_v4(), 'Puma',      'puma',      NULL, TRUE),
    (uuid_generate_v4(), 'Mavi',      'mavi',      NULL, TRUE),
    (uuid_generate_v4(), 'LC Waikiki','lc-waikiki',NULL, TRUE),
    (uuid_generate_v4(), 'Koton',     'koton',     NULL, TRUE),
    (uuid_generate_v4(), 'Defacto',   'defacto',   NULL, TRUE),
    (uuid_generate_v4(), 'Arçelik',   'arcelik',   NULL, TRUE),
    (uuid_generate_v4(), 'Vestel',    'vestel',    NULL, TRUE),
    (uuid_generate_v4(), 'Bosch',     'bosch',     NULL, TRUE);
