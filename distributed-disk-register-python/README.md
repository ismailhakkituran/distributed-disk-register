# Distributed Disk Register - HaToKuSe Protokolü

**Python ile gRPC ve Socket Programlama Kullanılarak Geliştirilmiş Hata-Tolere Dağıtık Mesaj Kayıt Sistemi**

## 📋 Proje Hakkında

Bu proje, Sistem Programlama dersi kapsamında geliştirilmiş, hata-tolere (fault-tolerant) dağıtık bir mesaj kayıt sistemidir. Sistem, **HaToKuSe (Hata-Tolere Kuyruk Servisi)** protokolü ile çalışır ve RabbitMQ ve Apache Kafka gibi sistemlerden ilham alınarak tasarlanmıştır.

## 🏗️ Sistem Mimarisi

### 1. **Lider (Leader/Coordinator)**
- İstemcilerle **TCP Socket** üzerinden metin tabanlı (SET/GET) iletişim kurar
- Aile üyeleriyle (Nodes) **gRPC + Protocol Buffers** üzerinden binary iletişim kurar
- `tolerance.conf` dosyasındaki değere göre veriyi çoğaltır (Replication Factor)
- **Akıllı Yük Dengelemesi (Load Balancing):** En az mesaj sayısına sahip node'lara öncelik vererek veriyi dengeli dağıtır
- Her mesajın hangi node'larda saklandığını `leader_metadata/message_mapping.txt` dosyasında tutar
- Periyodik olarak (10 saniyede bir) sistem durumunu ve istatistikleri raporlar
- Sistem yeniden başlatıldığında metadata'yı diskten yükler ve node'lardaki mevcut mesajları keşfeder

### 2. **Aile Üyeleri (Worker Nodes)**
- Liderden gelen verileri gRPC üzerinden alır ve yerel diskinde saklar
- Dinamik olarak sisteme katılabilir (hot-plug support)
- İki farklı disk yazma modu destekler:
  - **Buffered IO:** Performans odaklı, Python standart buffer kullanır
  - **Unbuffered IO:** Güvenlik odaklı, doğrudan OS çağrıları yapar
- Periyodik olarak (5 saniyede bir) kendi istatistiklerini raporlar
- Her mesajı `storage_node_<id>/<message_id>.txt` formatında saklar

### 3. **İstemci (Client)**
- Metin tabanlı komutlarla (SET/GET) lidere bağlanır
- TCP Socket üzerinden senkron iletişim kurar

## 🛠️ Yapılan İşlemler (Tamamlanan Özellikler)

### ✅ 1. Protobuf Tasarımı ve Kod Üretimi
- `proto/family.proto` dosyası oluşturuldu
- ChatMessage, NodeInfo, StoreRequest/Response, GetRequest/Response, RegisterNodeRequest/Response mesaj yapıları tanımlandı
- FamilyService için StoreMessage, GetMessage, RegisterNode RPC servisleri tasarlandı
- `protoc` ile Python kodları (`generated/family_pb2.py` ve `generated/family_pb2_grpc.py`) üretildi

### ✅ 2. Lider (Server) Geliştirme
- **LeaderService** sınıfı implement edildi:
  - Tolerance seviyesi konfigurasyon dosyasından okunuyor (`tolerance.conf`)
  - Node kayıt sistemi (`RegisterNode` RPC)
  - Mesaj saklama sistemi (yük dengelemeli dağıtım)
  - Mesaj okuma sistemi (hata toleranslı, çöken node'ları atlar)
  - Metadata yönetimi (disk üzerinde kalıcılık)
  - Node discovery mekanizması (yeni node'lardaki mevcut mesajları keşfetme)
  
- **Socket Sunucusu:**
  - TCP Socket ile istemcilere hizmet verir
  - Multi-threaded yapı (her client için ayrı thread)
  - SET ve GET komutlarını parse eder ve işler
  
- **Periyodik Raporlama:**
  - 10 saniyede bir terminal ekranını temizleyerek canlı istatistik gösterir
  - Toplam mesaj sayısı, aktif node sayısı ve her node'un mesaj dağılımı

### ✅ 3. Worker Node Geliştirme
- **WorkerNode** sınıfı implement edildi:
  - StoreMessage ve GetMessage RPC metodları
  - İki farklı disk IO modu (buffered/unbuffered)
  - Otomatik storage klasörü oluşturma
  
- **Node Kaydı:**
  - Başlatıldığında otomatik olarak lidere kayıt olur
  - gRPC channel üzerinden lider ile iletişim kurar
  
- **Periyodik Raporlama:**
  - 5 saniyede bir kendi istatistiklerini gösterir
  - Terminal ekranını temizleyerek daha okunabilir çıktı sağlar

### ✅ 4. İstemci Geliştirme
- TCP Socket üzerinden lidere bağlanır
- İnteraktif komut satırı arayüzü
- SET, GET ve EXIT komutlarını destekler
- Sunucu yanıtlarını ekrana yazdırır

### ✅ 5. Ana Program (main.py)
- Unified entry point: Hem lider hem de node'lar aynı programdan başlatılabilir
- Komut satırı argüman parsing (--mode, --id, --port, --io-mode)
- Kullanıcı dostu hata mesajları

### ✅ 6. Yük Dengeleme Algoritması
- **Akıllı Dağıtım:** Her SET işleminde node'lar mesaj sayısına göre sıralanır
- En az mesajı olan tolerance sayısı kadar node seçilir
- Bu sayede sistem otomatik olarak dengeli bir dağılım yapar
- Yeni eklenen node'lar zamanla daha fazla mesaj alarak sistemi dengeler

### ✅ 7. Hata Toleransı Mekanizması
- Bir veya birden fazla node çökse bile sistem çalışmaya devam eder
- GET işlemi sırasında çöken node'lar atlanır, hayatta olan node'lardan veri okunur
- Metadata sistemi sayesinde hangi node'larda hangi mesajların olduğu bilinir
- Node discovery ile sisteme sonradan katılan node'lardaki veriler keşfedilir

### ✅ 8. Disk IO Optimizasyonları
- **Buffered IO:** Python'un standart 8KB buffer'ı ile performanslı yazma
- **Unbuffered IO:** `os.open()` ve `os.write()` ile direkt OS çağrıları
- Kullanıcı başlangıçta seçebiliyor (--io-mode parametresi)

### ✅ 9. Kalıcılık (Persistence)
- Lider metadata'yı `leader_metadata/message_mapping.txt` dosyasında saklar
- Node'lar her mesajı ayrı dosya olarak saklar
- Sistem yeniden başlatıldığında metadata yüklenir ve node'lar keşfedilir

### ✅ 10. Dinamik Node Yönetimi
- Node'lar çalışırken sisteme eklenebilir
- Node'lar çökse bile sistem devam eder
- Yeni node'lar otomatik olarak kayıt olur ve yük dengelemeye katılır

## 🚀 Kurulum ve Çalıştırma

### Gereksinimler
```bash
pip install grpcio grpcio-tools
```

### 1. Lider (Coordinator) Başlatma
```bash
python src/main.py --mode leader
```
veya
```bash
python src/server.py
```

### 2. Node'ları (Workers) Başlatma
Her node için ayrı terminal açın:
```bash
# Node 1
python src/main.py --mode node --id 1 --port 5555

# Node 2
python src/main.py --mode node --id 2 --port 5556

# Node 3
python src/main.py --mode node --id 3 --port 5557

# Node 4
python src/main.py --mode node --id 4 --port 5558
```

**Unbuffered IO ile başlatma:**
```bash
python src/main.py --mode node --id 1 --port 5555 --io-mode unbuffered
```

### 3. İstemci Başlatma
```bash
python src/client.py
```

### 4. Komutlar
```
> SET 1 Merhaba Dünya
Sunucu Yaniti: OK

> GET 1
Sunucu Yaniti: VALUE Merhaba Dünya

> EXIT
```

## 📊 Test Senaryoları

### Test 1: Tolerance=2, 4 Node
**Amaç:** Temel hata toleransı ve yük dengeleme testi

**Yapılandırma:**
- `tolerance.conf` dosyasında `tolerance=2` olarak ayarlanır
- 1 lider + 4 worker node başlatılır
- 1000 SET mesajı gönderilir

**Beklenen Sonuçlar:**
- ✅ Her mesaj 2 farklı node'a kaydedilir
- ✅ Dengeli dağılım: Her node ~500 mesaj alır (1000*2/4=500)
- ✅ Bir node çöktüğünde GET işlemi diğer node'dan başarılı olur

### Test 2: Tolerance=3, 6 Node
**Amaç:** Yüksek hata toleransı ve çoklu node crash testi

**Yapılandırma:**
- `tolerance.conf` dosyasında `tolerance=3` olarak ayarlanır
- 1 lider + 6 worker node başlatılır
- 9000 SET mesajı gönderilir

**Beklenen Sonuçlar:**
- ✅ Her mesaj 3 farklı node'a kaydedilir
- ✅ İki üçlü grup oluşur, her grupta 4500 mesaj (9000*3/6=4500)
- ✅ 2 node crash olsa bile kalan 1 node'dan mesaj okunabilir

### Test 3: Dinamik Node Ekleme
**Amaç:** Hot-plug desteği ve otomatik yük dengeleme

**Senaryo:**
1. Tolerance=2 ile 3 node başlatılır
2. 500 mesaj gönderilir
3. Yeni bir node (Node 4) sisteme eklenir
4. 500 mesaj daha gönderilir

**Beklenen Sonuçlar:**
- ✅ Yeni node otomatik olarak kayıt olur
- ✅ İkinci 500 mesaj yeni node'u da kullanır
- ✅ Sistem dengeli dağılıma doğru evrilir

## 📁 Proje Yapısı

```
distributed-disk-register-python/
├── proto/
│   └── family.proto              # Protobuf tanımları
├── generated/
│   ├── family_pb2.py            # Üretilen protobuf kodları
│   └── family_pb2_grpc.py       # Üretilen gRPC kodları
├── src/
│   ├── main.py                  # Ana program (unified entry point)
│   ├── server.py                # Lider implementasyonu
│   ├── node.py                  # Worker node implementasyonu
│   └── client.py                # İstemci programı
├── leader_metadata/
│   └── message_mapping.txt      # Mesaj-node eşleşmeleri
├── storage_node_1/              # Node 1'in disk alanı
├── storage_node_2/              # Node 2'nin disk alanı
├── tolerance.conf               # Hata tolerans konfigürasyonu
└── README.md                    # Bu dosya
```

## 🔍 Teknik Detaylar

### İletişim Protokolleri
- **İstemci ↔ Lider:** TCP Socket (metin tabanlı)
  - Port: **6666** (Java örneğiyle aynı)
  - Format: `SET <id> <mesaj>` veya `GET <id>`
  - Yanıt: `OK`, `ERROR` veya `VALUE <mesaj>`

- **Lider ↔ Node'lar:** gRPC + Protocol Buffers (binary)
  - Lider gRPC Port: **5550**
  - Node Portları: **5555, 5556, 5557, 5558...** (Java örneğiyle aynı range)
  - StoreMessage: Mesaj kaydetme
  - GetMessage: Mesaj okuma
  - RegisterNode: Node kaydı

### Thread Modeli
- **Lider:** 
  - Ana thread: Socket sunucusu (accept loop)
  - Client thread'leri: Her client için ayrı thread
  - Rapor thread'i: Daemon thread, periyodik raporlama
  - gRPC thread pool: ThreadPoolExecutor (max_workers=10)

- **Node:**
  - Ana thread: gRPC sunucusu
  - Rapor thread'i: Daemon thread, periyodik raporlama

### Senkronizasyon
- Lider'de `threading.Lock()` ile critical section koruması
- Node'ların kayıt listesi ve message_to_nodes mapping'leri lock altında güncellenir

### Disk Formatı
- Her mesaj ayrı dosya: `<message_id>.txt`
- Metadata formatı: `<message_id>:<node_id1>,<node_id2>,...\n`

## 🎯 Ödev Gereksinimleri Karşılama Durumu

| Gereksinim | Durum | Açıklama |
|------------|-------|----------|
| ✅ gRPC kullanımı | Tamamlandı | Lider-Node arası iletişim |
| ✅ HaToKuSe protokolü | Tamamlandı | Socket üzerinden SET/GET |
| ✅ Dağıtık saklama | Tamamlandı | Tolerance seviyesine göre replikasyon |
| ✅ Hata toleransı | Tamamlandı | Node crash'lerinde sistem devam eder |
| ✅ Yük dengeleme | Tamamlandı | En az mesajı olan node'lara öncelik |
| ✅ Tolerance=1,2 | Tamamlandı | Test edildi ve çalışıyor |
| ✅ Tolerance=n (max 7) | Tamamlandı | Dinamik olarak ayarlanabilir |
| ✅ Disk IO çeşitleri | Tamamlandı | Buffered ve Unbuffered modlar |
| ✅ Dinamik node yönetimi | Tamamlandı | Hot-plug desteği |
| ✅ Lider metadata takibi | Tamamlandı | Disk üzerinde kalıcılık |
| ✅ Periyodik raporlama | Tamamlandı | Lider ve node'lar rapor verir |
| ✅ README dokümantasyonu | Tamamlandı | Bu dosya |

## 👨‍💻 Geliştirme Notları

### Özgün Tasarım Kararları
1. **Akıllı Node Seçimi:** Sadece round-robin değil, mesaj sayısına göre dinamik seçim
2. **Node Discovery:** Sistem yeniden başlatıldığında node'lardaki mevcut veriler otomatik keşfedilir
3. **Canlı Raporlama:** Terminal temizleyerek sürekli güncel istatistikler gösterilir
4. **Unified Entry Point:** Tek program ile hem lider hem node başlatılabilir

### Performans Optimizasyonları
- Buffered IO ile disk yazma performansı artırıldı
- Thread pool ile eşzamanlı istek işleme
- Metadata caching ile disk okuma azaltıldı

### Güvenlik ve Dayanıklılık
- Unbuffered IO ile veri kaybı riski azaltıldı
- Metadata'nın disk üzerinde tutulması ile sistem yeniden başlatmada veri kurtarma
- Exception handling ile robust hata yönetimi

## 📝 Lisans
Bu proje eğitim amaçlı geliştirilmiştir.

## 🙏 Teşekkürler
RabbitMQ ve Apache Kafka projelerine ilham için teşekkürler.
