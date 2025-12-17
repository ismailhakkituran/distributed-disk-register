## 1. Aşama
* [ ] Grup üyeleri girişini yap.
* [ ] GitHub’daki şablon repoyu **fork** et
* [ ] Github' da ekip için proje oluştur. Taskları burada tanımla ve ekip üyelerine ata.
* [ ] TCP server istemciden gelen mesajları pars etme SET/GET Komut parse eden bir sınıf. Örn: `Command parse(String line)` → `SetCommand` / `GetCommand`

  * `SET` → `map.put(id, msg)` + `OK`
  * `GET` → `map.get(id)` + bulunamazsa `NOT_FOUND`


## 2. Aşama – Diskte Mesaj Saklama (Tek Node)

**Amaç:** Disk IO, buffered/unbuffered fikrine giriş.

* [ ] `messages/` klasöründe her mesajı **ayrı dosyada** tut:

  * Örn. `messages/42.msg` içinde sadece o mesajın içeriği
* [ ] `SET <id> <msg>`: Diskte dosya oluştur / üzerine yaz
* [ ] `GET <id>`: İlgili dosyayı aç, içeriği oku, istemciye dön
* [ ] İki farklı IO modu araştırılabilir:

  * [ ] **Buffered IO** ile yaz/oku (örn. `BufferedWriter`, `BufferedReader`)
  * [ ] **Unbuffered IO** (doğrudan `FileOutputStream`, `FileInputStream`)


  * Buffered vs unbuffered farkı nedir, hangi durumda daha avantajlıdır?

  * 2.Aşamada zaman kaybetmemek için tipik dosyaya yazma işlemi ile bitirip, daha sonra buraya dönebilirsiniz.
---

## 3. Aşama – gRPC Mesaj Modeli (Protobuf Nesnesi)

**Amaç:** Lider ile üyeler arasındaki mesajın Protobuf ile modellenmesi.

**Görevler:**

* [ ] `.proto` dosyasında aşağıya benzer bir message tanımlanabilir:

  ```proto
  message StoredMessage {
    int32 id = 1;
    string text = 2;
  }
  ```

* [ ] Üyelerin diske kaydedeceği mesajı temsil eden Java sınıfları artık Protobuf’dan gelsin:

  * `StoredMessage` ile çalışın, text + id bir arada olsun.

* [ ] gRPC servis iskeleti oluştur:

  * Örn: `service StorageService { rpc Store(StoredMessage) returns (StoreResult); rpc Retrieve(MessageId) returns (StoredMessage); }`

* [ ] Henüz dağıtık yapmayın:

  * Lider ve üye aynı process içinde de olabilir, amaç önce gRPC fonksiyonunu ayağa kaldırmak.


---

## 4. Aşama – Tolerance=1 ve 2 için Dağıtık Kayıt

**Amaç:** Hata toleransı 1 ve 2 için **temel dağıtık kayıt sistemi**.

**Görevler:**

* [ ] `tolerance.conf` dosyasını okuyun:

  * İçinde tek satır olsun: `TOLERANCE=2`
* [ ] Lider, her SET isteğinde:

  1. Gelen id+mesajı diske kaydetsin (kendi mesaj haritasına da eklesin)
  2. Üye listesinden tolerance sayısı kadar üye seçsin:

     * Tolerance=1 → 1 üye
     * Tolerance=2 → 2 üye
  3. Bu üyelere gRPC ile `Store(StoredMessage)` RPC’si göndersin
  4. Hepsinden başarılı yanıt geldiyse istemciye `OK`
  5. Bir veya daha fazlası başarısız olursa:

     * Bu durumda ne yapılacağı (retry, ERROR, vb) takım tasarımına bırakılabilir
* [ ] Lider, “mesaj id → hangi üyelerde var” bilgisini bir map’te tutsun:

  * `Map<Integer, List<MemberId>>`
* [ ] GET isteğinde:

  * Eğer liderin kendi diskinde varsa doğrudan kendinden okusun
  * Yoksa mesajın tutulduğu üye listesinden sırayla gRPC ile `Retrieve` isteği göndersin
  * İlk cevap veren (ya da hayatta kalan) üyeden mesajı alıp istemciye döndürsün

> Bu aşama bittiğinde:
> **“Hata toleransı 1 ve 2 olarak çalışan dağıtık kayıt sistemi”** kısmı büyük oranda tamam.

---

## 6. Aşama – Hata Toleransı n (Genel Hâl) ve Load Balancing

**Amaç:** Tolerance=1,2,3,…,7 için genel çözüm + dengeli dağılım.

**Görevler:**

* [ ] `tolerance.conf` içindeki değeri **yapılandırılabilir** hale getirin (1..7)
* [ ] Test senaryoları:

  * Tolerance=2, 5 üye → her mesaj 2 üyeye gitsin
  * Tolerance=3, 7 üye → her mesaj 3 üyeye gitsin
* [ ] Mesaj dağılımı:

  * `message_id` veya **round-robin** ile üyeleri seçebilirsiniz
  * Amaç: Çok sayıda SET sonrası üyeler arası yük mümkün olduğunca dengeli olsun
  * Bunu ölçmek için:

    * [ ] Sonda her üyenin kaç mesaj sakladığını ekrana yazdıran fonksiyon ekleyin
* [ ] Test dokümantasyonu:

  * 1000 SET sonrası 2 set üye için 500-500 civarı mesaj dağılımı
  * 9000 SET sonrası iki üçlü grup için 4500-4500 civarı dağılım

---

## 7. Aşama – Crash Senaryoları ve Recovery

**Amaç:** Test senaryosu 1 & 2’de tarif edilen crash durumlarını simüle etmek.

**Görevler:**

* [ ] Üye proceslerinden birini manuel kapatın (kill, terminal kapama)
* [ ] Lider:

  * GET sırasında crash olmuş üyeye bağlanmaya çalışırken exception aldığında:

    * O üyeyi “dead” işaretlesin (veya listeden çıkarsın)
    * Listedeki diğer üye(ler)den mesajı okumayı denesin
* [ ] Test 1:

  * Tolerance=2, 4 üye
  * Mesaj id 500 → üye 3 ve 4’te kayıtlı olsun
  * Üye 3’ü kapat → lider GET 500 isteğini üye 4’ten çekebiliyor mu?
* [ ] Test 2:

  * Tolerance=3, 6 üye
  * Mesaj id 4501 → üye 3,5,6’da
  * 1 veya 2 üye crash olsa bile, hayatta kalan son üyeden GET yapılabiliyor mu?

Bu aşamada amaç: **gerçekten fault-tolerant** çalıştığını gösterecek küçük test senaryosu log’ları üretmek.

---
