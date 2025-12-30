# Dağıtık Mesaj Kayıt Servisi Projesi Notları
Bu proje, verilerin hem güvenli bir şekilde saklandığı hem de farklı sunucular arasında paylaşıldığı dağıtık bir kayıt sistemidir. Bir sunucu kapansa ya da bozulsa bile verilerin kaybolmamasını sağlar. Dışarıdan bir kullanıcı sisteme bağlanıp düz metin(TCP) şeklinde bir mesaj gönderdiğinde, bu isteği grubun 'Lider' sunucusu karşılar.Lider sunucu bir veri aldığında, bunu gruptaki diğer sunuculara (düğümlere) iletmek için gRPC ve Protobuf teknolojisini kullanır.

1.Aşama- Komut Ayrıştırma ve TCP İşleme Mantığı:

İstemci, SET <ID> <Mesaj> veya GET <ID> şeklinde iki farklı istek gönderebiliyor. Bu gelen isteği parse ederek SetCommand mı yoksa GetCommand mı olduğuna karar veriyor, eğer komut SET ise; gelen mesaj ID ile eşleştirilerek sisteme kaydediliyor ve kullanıcıya "OK" cevabı dönülüyor. Eğer komut GET ise; sistemde bu ID'ye karşılık gelen bir mesaj olup olmadığına bakılıyor. Veri varsa mesajın kendisi, yoksa "NOT_FOUND" cevabı gönderiliyor.

2.Aşama- Diskte Mesaj Saklama:

