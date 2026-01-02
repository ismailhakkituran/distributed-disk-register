# Dağıtık Mesaj Kayıt Servisi Projesi Notları
Bu proje, verilerin hem güvenli bir şekilde saklandığı hem de farklı sunucular arasında paylaşıldığı dağıtık bir kayıt sistemidir. Bir sunucu kapansa ya da bozulsa bile verilerin kaybolmamasını sağlar. Dışarıdan bir kullanıcı sisteme bağlanıp düz metin(TCP) şeklinde bir mesaj gönderdiğinde, bu isteği grubun 'Lider' sunucusu karşılar.Lider sunucu bir veri aldığında, bunu gruptaki diğer sunuculara (düğümlere) iletmek için gRPC ve Protobuf teknolojisini kullanır.

1.Aşama- Komut Ayrıştırma ve TCP İşleme Mantığı:

İstemci, SET <ID> <Mesaj> veya GET <ID> şeklinde iki farklı istek gönderebiliyor. Bu gelen isteği parse ederek SetCommand mı yoksa GetCommand mı olduğuna karar veriyor, eğer komut SET ise; gelen mesaj ID ile eşleştirilerek sisteme kaydediliyor ve kullanıcıya "OK" cevabı dönülüyor. Eğer komut GET ise; sistemde bu ID'ye karşılık gelen bir mesaj olup olmadığına bakılıyor. Veri varsa mesajın kendisi, yoksa "NOT_FOUND" cevabı gönderiliyor.

2.Aşama- Diskte Mesaj Saklama:

Bu aşamada, sistemdeki verilerin geçici bellek (RAM) yerine kalıcı bir depolama biriminde (Disk) saklanmasını sağlayarak herhangi bir çökme veya yeniden başlatma durumunda veri kaybının önüne geçilmesi amaçlanmıştır. SET komutu tetiklendiğinde iletilen veri sadece o anlık işlenmekle kalmayıp DiskManager aracılığıyla messages dizini altında ilgili ID’ye özel bir .msg dosyası oluşturulmakta veya güncellenmektedir. GET komutu geldiğinde ise sistem, bu dosya yapısı üzerinden ilgili kayda ulaşarak veriyi diskten geri çağırmaktadır. Veri yazma ve okuma performansını optimize etmek amacıyla doğrudan işletim sistemine çağrı yapan "Unbuffered I/O" yöntemi yerine, disk erişim maliyetini minimize eden Buffered I/O (Tamponlu G/Ç) mimarisi tercih edilmiştir. BufferedWriter ve BufferedReader kullanımı sayesinde veriler doğrudan diske gönderilmek yerine önce bellekteki bir tamponda toplanmakta böylece sistem çağrıları azaltılarak özellikle sık tekrarlanan işlemlerde yüksek performans ve veri tutarlılığı elde edilmektedir.

3.Aşama- gRPC Mesaj Modeli

Bu aşamada , iki bilgisayarın birbiriyle anlaşabilmesi için ortak dil belirledik. (family.proto) Sunucuların birbirine ne göndereceğini sisteme tanıttık ayrıca sunucunun dışarıya hangi fonksiyonları açacağını belirledik. (StorageServiceImpl.java) gRPC üzerinden bir kaydet isteği geldiğinde onu yakalayıp 2. aşamada olan  DiskManager'a teslim eden  yani ağdan gelen veriyi diske yazdıran köprüyü kurmuş olduk.Bu aşamada son olarak da sunucu ayağa kalkarken depolama servisinide çalıştır kodunu ekledik.

4. Aşama - Dağıtık Lider Mantığı

Bu aşamada , önce sisteme esneklik kazandırmak için kaç kişiye göndereceğimizi  kodun içine yazmak yerine tolerance.conf diye bir ayar dosyası oluşturduk . Sunucu başlarken bu dosyayı okuyor ve lider , tolerance değerine göre yedekleme yapıyor. NodeMain sınıfına distributionMap adında bir harita (Map) ekledik. Bu sayede lider veriyi diğer node'lara  başarıyla gönderdiyse bu haritaya not düşüyor . Ayrıca liderin diğer üyelere paket taşımasını sağlayan fonksiyonu da yazdık bu fonksiyon önce tüm üyelerin listesini alıyor ama kendini çıkartıyor , kalan üyeler arasından tolerance sayısı kadar yedek sunucu seçiyor . Sonra seçilen bu sunuculara  Store servsi üzerinden bağlanıp veriyi gönderiyor. Seçilen sunucular okeylerse lider bunu not kaydediyor.

