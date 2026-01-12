Proje Adı: Dağıtık ve Hata Toleranslı Mesaj Kayıt Sistemi (HaToKuSe)
1. Proje Özeti
Bu proje, Sistem Programlama dersi kapsamında; dağıtık sistemlerin temel prensipleri olan Lider Seçimi (Leader Election), Veri Replikasyonu (Replication), Yük Dengeleme (Load Balancing) ve Hata Toleransı (Fault Tolerance) kavramlarını hayata geçirmek amacıyla geliştirilmiştir.
Sistem, merkezi bir lider sunucu ve bu lidere bağlı, dinamik olarak ölçeklenebilen üye (node) sunuculardan oluşmaktadır. İstemciler ile lider arasındaki iletişim metin tabanlı (text-based) özel bir protokol üzerinden sağlanırken; sunucular arası (inter-node) iletişim, performans ve veri bütünlüğü açısından Google Protobuf ve gRPC teknolojileri kullanılarak binary formatta gerçekleştirilmiştir.
2. Sistem Mimarisi ve Tasarım Kararları
Sistem mimarisi, Lider-Takipçi (Master-Slave) topolojisine dayanmaktadır. Sistemin genel işleyiş mekanizması aşağıdaki gibidir:
•	Keşif ve Katılım (Discovery & Join): Sisteme dahil olan her düğüm, belirlenen port aralığını tarayarak mevcut bir lider olup olmadığını kontrol eder. Eğer bir lider mevcutsa gRPC Join prosedürü ile ağa dahil olur.
•	Lider Seçimi: Ağ üzerindeki düğümler arasında en düşük port numarasına (ID) sahip olan düğüm, NodeMain sınıfındaki algoritma gereği otomatik olarak lider statüsü kazanır.
•	Protokol Ayrımı:
o	External (Client-to-Leader): İstemcilerin basit telnet/netcat araçlarıyla bağlanabilmesi için TCP soketleri üzerinden SET ve GET komutlarını işleyen metin tabanlı bir parser geliştirilmiştir.
o	Internal (Node-to-Node): Düğümler arası veri transferinde, tip güvenliği ve serileştirme hızı nedeniyle gRPC tercih edilmiştir.
3. Teknik Gerçekleştirim Detayları
3.1. Veri Kalıcılığı ve Disk I/O Yönetimi
Projenin temel gereksinimlerinden biri olan disk operasyonları, DiskManager sınıfı içerisinde modüler bir yapıda kurgulanmıştır. tolerance.conf dosyası üzerinden yapılandırılabilen üç farklı I/O stratejisi implemente edilmiştir:
1.	Buffered I/O: BufferedWriter kullanılarak verilerin bellekte tamponlanıp diske yazılması sağlanmıştır. Genel kullanım için varsayılan moddur.
2.	Unbuffered I/O: İşletim sistemi çağrılarını doğrudan tetikleyen FileOutputStream kullanımıdır. Veri bütünlüğünün kritik olduğu durumlarda test edilmiştir.
3.	Zero Copy (NIO): Java NIO (New I/O) kütüphanesindeki FileChannel yapısı kullanılarak, verinin user-space ile kernel-space arasında minimum kopyalama işlemiyle transfer edilmesi sağlanmıştır. Yüksek performans gerektiren durumlar için eklenmiştir.
4. Proje Geliştirme Süreci ve Aşamalar
Sistem geliştirme süreci, modüler bir yaklaşımla aşağıdaki teknik aşamalar takip edilerek tamamlanmıştır:
1. Aşama: Temel TCP Sunucusu ve Komut Ayrıştırma
•	İstemcilerin sisteme metin tabanlı komutlarla erişebilmesi sağlandı.
•	CommandParser: Gelen ham metin verisini (String) anlamlı komut nesnelerine (SetCommand, GetCommand) dönüştüren yapı kuruldu.
•	SET <id> <msg> ve GET <id> formatları desteklendi.
2. Aşama: Disk Yönetimi ve I/O İşlemleri
•	Verilerin kalıcı olarak saklanması için DiskManager sınıfı geliştirildi.
•	Her mesaj, messages/ dizini altında kendi ID'si ile (100.msg gibi) izole edilmiş dosyalarda saklanarak yönetim kolaylığı sağlandı.
3. Aşama: Veri Modelleme (Protobuf & gRPC)
•	Düğümler arası veri transferi için Google Protocol Buffers (Protobuf) entegrasyonu yapıldı.
•	Mesaj yapısı .proto dosyasında StoredMessage (id ve text içeren) olarak modellendi.
•	Düğümlerin birbirine veri aktarması için StorageService gRPC servisi tanımlandı.
4. Aşama: Temel Replikasyon (Tolerans=1 ve 2)
•	Sisteme hata toleransı özelliği eklendi.
•	tolerance.conf dosyasından okunan yapılandırma değerine göre, liderin gelen mesajı belirlenen sayıda yedek düğüme (replica) kopyalaması sağlandı.
•	Lider düğüm, hangi mesajın hangi üyelerde tutulduğunu bellek üzerinde (messageLocationMap) takip etmeye başladı.
6. Aşama: Yük Dengeleme (Load Balancing) ve Sistem İzleme
•	Sistem, N sayıda toleransı destekleyecek ve yükü eşit dağıtacak şekilde güncellendi.
•	Round-Robin Algoritması: Lider düğümün her mesajı belirli düğümlere yığması yerine; lider dahil tüm üyelerin oluşturduğu havuzda döngüsel (Round-Robin) dağıtım mekanizması kuruldu. Bu sayede veri depolama yükü küme (cluster) genelinde dengelendi.
7. Aşama: Crash Senaryoları ve Recovery (Failover)
•Sistemin dayanıklılığını test etmek amacıyla, veri tutan düğümlerin ani kapanma (Crash) senaryoları simüle edildi.
•Lider sunucunun, GET isteği sırasında çökmüş bir düğümü tespit edip (Exception Handling), veriyi otomatik olarak ayakta kalan diğer replikadan çekmesi (Failover) sağlandı.

GRUP ÜYELERİ
•23060490 - Rukiye Sıla Aslan
•23060816 - Hayrunnisa Köle
