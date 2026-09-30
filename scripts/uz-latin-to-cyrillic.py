"""O'zbekcha (lotin) bot matnlaridan kirill yozuvidagi faylni yaratadi.

Ishlatish (loyiha ildizidan):
    python scripts/uz-latin-to-cyrillic.py

messages_uz.properties -> messages_cy.properties. HTML teglar, {o'zgaruvchi}lar,
/buyruqlar, \\n kabi escape'lar va KEEP ro'yxatidagi so'zlar o'zgartirilmaydi.
Oy nomlari kabi o'zlashma so'zlar OVERRIDES orqali to'g'ri yoziladi.
Yangi kalit qo'shganda shu skriptni qayta ishga tushiring.
"""
import io
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/resources/bot/i18n/messages_uz.properties"
DST = ROOT / "src/main/resources/bot/i18n/messages_cy.properties"

KEEP = {"QR", "Start", "Telegram", "BotFather", "Maktab Boshqaruv"}
OVERRIDES = {
    "yanvar": "январ", "fevral": "феврал", "aprel": "апрел", "iyun": "июн", "iyul": "июл",
    "sentabr": "сентябр", "oktabr": "октябр", "noyabr": "ноябр", "dekabr": "декабр",
    "yakshanba": "якшанба", "grafik": "график", "direktor": "директор",
}
SIMPLE = {
    "a": "а", "b": "б", "d": "д", "f": "ф", "g": "г", "h": "ҳ", "i": "и", "j": "ж", "k": "к",
    "l": "л", "m": "м", "n": "н", "o": "о", "p": "п", "q": "қ", "r": "р", "s": "с", "t": "т",
    "u": "у", "v": "в", "x": "х", "y": "й", "z": "з", "c": "с", "w": "в",
}
APOS = "'‘ʻ’`"
SKIP = re.compile(r"(<[^>]*>|\{[^}]*\}|&[a-z]+;|/[a-z_]+|\\[nt ]|https?://\S+)")
WORD = re.compile(r"[A-Za-z" + APOS + r"]+")


def case_like(src, dst):
    if src.isupper() and len(src) > 1:
        return dst.upper()
    if src[:1].isupper():
        return dst[:1].upper() + dst[1:]
    return dst


def translit_word(word):
    if word in KEEP:
        return word
    low = word.lower()
    for latin, cyr in OVERRIDES.items():
        if low == latin:
            return case_like(word, cyr)
    out = []
    i = 0
    while i < len(word):
        ch = word[i]
        lo = ch.lower()
        nxt = word[i + 1].lower() if i + 1 < len(word) else ""
        nxt2 = word[i + 2] if i + 2 < len(word) else ""
        piece = None
        step = 1
        if lo in "og" and nxt and nxt in APOS:
            piece, step = ("ў" if lo == "o" else "ғ"), 2
        elif lo == "s" and nxt == "h":
            piece, step = "ш", 2
        elif lo == "c" and nxt == "h":
            piece, step = "ч", 2
        elif lo == "y" and nxt in ("o", "u", "a", "e") and not (nxt == "o" and nxt2 in APOS):
            piece, step = {"o": "ё", "u": "ю", "a": "я", "e": "е"}[nxt], 2
        elif lo == "e":
            piece = "э" if i == 0 else "е"
        elif ch in APOS:
            piece = "ъ" if i > 0 else ch
        else:
            piece = SIMPLE.get(lo, ch)
        src = word[i:i + step]
        out.append(case_like(src, piece) if src[:1].isalpha() else piece)
        i += step
    return "".join(out)


def translit_text(text):
    parts = SKIP.split(text)
    result = []
    for idx, part in enumerate(parts):
        if idx % 2 == 1:  # a SKIP match
            result.append(part)
        else:
            protected = part
            for phrase in KEEP:
                protected = protected.replace(phrase, "\x00" + phrase.replace(" ", "\x01") + "\x00")
            chunks = protected.split("\x00")
            for j, chunk in enumerate(chunks):
                if j % 2 == 1:
                    result.append(chunk.replace("\x01", " "))
                else:
                    result.append(WORD.sub(lambda m: translit_word(m.group(0)), chunk))
    return "".join(result)


def main():
    lines = io.open(SRC, encoding="utf-8").read().split("\n")
    out = ["# AVTOMATIK YARATILGAN - qo'lda tahrirlamang. Manba: messages_uz.properties,",
           "# skript: scripts/uz-latin-to-cyrillic.py. O'zbekcha (kirill) matnlar."]
    for line in lines:
        if not line or line.startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if key.startswith("lang."):
            out.append(line)  # language names are shown as-is in every language
            continue
        out.append(key + sep + translit_text(value))
    io.open(DST, "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
    print(f"{DST.name}: {len(out) - 2} ta kalit yozildi")


if __name__ == "__main__":
    main()
