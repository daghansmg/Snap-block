# Sohbet Devir Dosyası — Evren LLM Oturumu

> Tarih: 23–26 Kasım 2026 · Repo: `daghansmg/Snap-block` · Dal: `claude/evren-llm-session-39pfiz`

Bu dosya, **bu sohbette ne konuşulduğunu, ne yapıldığını ve nerede kalındığını** özetler.
Yeni bir oturum ya da başka biri işe buradan devam edebilir.

---

## 1. Kısa özet

- **Hedef:** "Evren" adlı bir yapay zekânın API'sine bağlanmak ve muhtemelen bunu SnapBlock uygulamasında kullanmak.
- **Sonuç:** Bağlantı **kurulamadı**, çünkü API'nin adresi (host), endpoint'i ve kimlik doğrulama biçimi bilinmiyor.
- **Üretilenler:** Proje devir dosyası `DEVIR.md` (commit `80723f5`) ve bu dosya.

---

## 2. Adım adım yapılanlar

### Adım 1 — Kullanıcı bir anahtar yapıştırdı
- **Kullanıcı:** Başka açıklama olmadan `evren_llm_…` ile başlayan bir dize gönderdi.
- **Yapılan:**
  - Repo incelendi. Bir Android/Kotlin projesi; içinde "evren" ya da LLM'e dair hiçbir referans yok.
  - Dize bir API anahtarı olarak değerlendirildi. **Hiçbir dosyaya yazılmadı, commit edilmedi, hiçbir yere gönderilmedi.**
  - Kullanıcıya üç şey önerildi: anahtarı **iptal edip yenilemesi**, bir dahaki sefere güvenli form ya da ortam secret'ı kullanması, ve ne yapmak istediğini netleştirmesi.

### Adım 2 — "Bu Evren yapay zekânın sayfası, bu AI'nın API'si, bağlan buna"
- **Yapılan:**
  - Webde "Evren LLM" API'si arandı. **Herkese açık bir doküman bulunamadı**; çıkan sonuçlar adı benzeyen, ilgisiz servislerdi.
  - Bağlantı için gereken üç bilgi kullanıcıdan istendi:
    1. Host adı (ör. `api.evren.…`)
    2. Endpoint ve istek/yanıt biçimi (OpenAI uyumlu mu?)
    3. Anahtarın nasıl gönderildiği (`Authorization: Bearer` mi, özel bir başlık mı)
  - Entegrasyon fikri ve ilk riskler paylaşıldı:
    - **Fikir:** BlockService engellemeden önce kişiyi ya da metni Evren'e sorsun.
    - **Risk:** Uygulamaya gömülen anahtar APK'dan çıkarılır. **Çözüm:** Araya backend proxy koymak.
    - **Risk:** Her ekranda API çağrısı yapmak pahalı ve yavaş. **Çözüm:** Önce cihazda ön filtre, Evren'e yalnızca belirsiz durumlar gitsin.
    - **Risk:** Kişisel veriler dışarı gider (KVKK). **Çözüm:** Veriyi maskelemek ve kullanıcıdan açık onay almak.
- **Durum:** Kullanıcı henüz link ya da host bilgisi vermedi.

### Adım 3 — "Devir dosyası oluştur"
- **Yapılan:**
  - Repo'daki tüm dosyalar okundu.
  - `DEVIR.md` yazıldı. İçinde projenin amacı, dosya haritası, çalışma akışı, derleme, durum, 9 risk ve çözümü, sıradaki adımlar var.
  - Commit `80723f5` → `claude/evren-llm-session-39pfiz` dalına push edildi.
- **Kullanıcının dönüşü:** "Hayır, bu sohbette yaptıklarımızın devir dosyasını ver" → bu dosya hazırlandı.

---

## 3. Alınan kararlar

| Karar | Gerekçe |
|---|---|
| Sohbete yapıştırılan anahtar **kullanılmadı ve kaydedilmedi** | Düz metin olarak açığa çıktı; güvenli değil. |
| Bilinmeyen bir host'a deneme isteği **atılmadı** | Adres tahmin edilirse anahtar yanlış bir yere gidebilirdi. |
| Anahtar ileride güvenli formla ya da ortam secret'ı olarak alınacak | Değer sohbete ve koda hiç girmez; proxy isteklere kendisi ekler. |

---

## 4. Açık işler (sırayla)

1. **[Kullanıcı]** Evren'in dokümantasyon linkini ya da API host adını ver.
2. **[Kullanıcı]** Sohbete yapıştırılan anahtarı iptal et ve yenisini üret.
3. **[Claude]** Host öğrenilince güvenli formla anahtarı al ve bir test isteği gönder.
4. **[Claude]** Bağlantı çalışırsa SnapBlock entegrasyon planını çıkar (backend proxy, maskeleme, onay akışı).
5. **[Opsiyonel]** `DEVIR.md`'deki proje risklerini ele al: Snapchat kullanım şartları, sabit koordinatlar, yalnızca Türkçe metinler, Gradle wrapper eksikliği.

---

## 5. Bu sohbette oluşan dosyalar

| Dosya | İçerik | Durum |
|---|---|---|
| `DEVIR.md` | SnapBlock projesinin teknik devri | Commit `80723f5`, push edildi |
| `SOHBET-DEVIR.md` | Bu sohbetin özeti (bu dosya) | Yeni |

> Not: Bu dosyada da, repo'nun hiçbir yerinde de API anahtarının değeri **yoktur**.
