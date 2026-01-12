# Test Klasörü

Bu klasör projenin test scriptlerini içerir.

## Test Dosyaları

### `test_load_distribution.py`
Mesaj dağılım testleri için kullanılır.

**Çalıştırma:**
```bash
cd tests
python test_load_distribution.py
```

**Ne yapar:**
- 1 Leader + 4 Node başlatır
- 100 mesaj gönderir
- Tolerans=2 ile her mesaj 2 node'a yazılır
- Dağılımı analiz eder (%25'er eşit dağılım beklenir)

**Test Sonuçları:**
- ✅ Toplam: 200 mesaj (100 × tolerans 2)
- ✅ Her node: 50 mesaj (%25)
- ✅ Mükemmel eşit dağılım
