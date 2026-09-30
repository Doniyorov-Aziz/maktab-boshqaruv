"""docs/NN-*.md fayllarini bitta docs/TOLIQ-DOKUMENTATSIYA.md ga yig'adi.

Ishlatish (loyiha ildizidan):  python docs/build-toliq.py

- Fayllar raqam tartibida birlashtiriladi.
- Har bir bo'limning yuqori/pastki navigatsiya qatorlari ("← ... · Keyingisi: ... →") olib tashlanadi.
- Bo'limlar ichidagi "## Mundarija" sarlavha emas, qalin matn bo'ladi — umumiy faylda
  bitta asosiy mundarija qoladi va anchor'lar to'qnashmaydi.
- Barcha havolalar (bo'lim ichidagi `#x` ham, fayllararo `05-malumotlar-bazasi.md#x` ham)
  o'sha bo'limdagi haqiqiy sarlavhaning GitHub anchor'iga aylantiriladi. Takroriy sarlavhalar
  (`#baza-1`) va `—`/`↔` belgilari tufayli paydo bo'ladigan `--` ham to'g'ri hisoblanadi.
"""
import io
import re
import sys
from collections import Counter
from pathlib import Path

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "TOLIQ-DOKUMENTATSIYA.md"
FILES = sorted(p for p in DOCS.glob("[0-9][0-9]-*.md"))
LINK = re.compile(r"\]\((\d\d-[\w-]+\.md)?(#[^)]*)?\)")
NAV = re.compile(r"^\s*(\[←|Keyingisi:|\[Hujjatlar ro'yxatiga)")
HEADING = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")
TOC_HEADING = re.compile(r"^##\s+Mundarija\s*$")


def slug(heading):
    """GitHub uslubidagi anchor: kichik harf, tinish belgilarsiz, har bir bo'shliq -> '-'."""
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", heading)
    return re.sub(r"[^\w\- ]", "", text.strip().lower()).replace(" ", "-")


def loose(anchor):
    """Havolani sarlavhaga moslashtirish uchun: ketma-ket '-' larni bittaga qisqartiradi."""
    return re.sub(r"-+", "-", anchor.lstrip("#").lower()).strip("-")


def headings(lines):
    """Kod bloklaridan tashqaridagi sarlavhalar."""
    fence = False
    for ln in lines:
        if ln.lstrip().startswith("```"):
            fence = not fence
        elif not fence and (m := HEADING.match(ln)):
            yield m.group(2)


def main():
    sections = []
    for p in FILES:
        lines = io.open(p, encoding="utf-8").read().rstrip("\n").split("\n")
        lines = [ln for ln in lines if not NAV.match(ln)]
        while lines and lines[-1].strip() in ("", "---"):
            lines.pop()
        lines = ["**Mundarija**" if TOC_HEADING.match(ln) else ln for ln in lines]
        sections.append((p.name, lines))

    title = "Maktab Boshqaruv — to'liq dokumentatsiya"
    # Umumiy fayldagi barcha sarlavhalar tartibida GitHub anchor'larini hisoblaymiz.
    seen = Counter([slug(title), "mundarija"])
    anchors = {}  # fayl -> {loose(anchor) -> haqiqiy anchor}; birinchi uchragani ustun
    for name, lines in sections:
        local = anchors.setdefault(name, {})
        for h in headings(lines):
            s = slug(h)
            real = s if seen[s] == 0 else f"{s}-{seen[s]}"
            seen[s] += 1
            local.setdefault(loose(s), real)
    first = {name: next(iter(local.values())) for name, local in anchors.items()}

    broken = []

    def relink(current):
        def sub(m):
            target, frag = m.group(1) or current, m.group(2)
            if target not in anchors:
                return m.group(0)
            if not frag:
                return f"](#{first[target]})"
            real = anchors[target].get(loose(frag))
            if real is None:
                broken.append(f"{current}: {m.group(0)}")
                return m.group(0)
            return f"](#{real})"
        return sub

    parts = [
        f"# {title}\n",
        "> Bu fayl avtomatik yig'ilgan (`python docs/build-toliq.py`) — uni qo'lda tahrirlamang, "
        "`docs/NN-*.md` fayllarini o'zgartirib, qayta yig'ing.\n",
        "## Mundarija\n",
        "\n".join(f"{i}. [{lines[0].lstrip('#').strip()}](#{first[name]})"
                  for i, (name, lines) in enumerate(sections, 1)) + "\n",
    ]
    for name, lines in sections:
        body = re.sub(r"\n{3,}", "\n\n", "\n".join(lines))
        parts.append("---\n\n" + LINK.sub(relink(name), body) + "\n")

    io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(parts))
    print(f"{OUT.name}: {len(sections)} ta bo'lim yig'ildi")
    if broken:
        print("Topilmagan havolalar:", *broken, sep="\n  ")
        sys.exit(1)


if __name__ == "__main__":
    main()
