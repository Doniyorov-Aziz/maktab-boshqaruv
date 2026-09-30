"""docs/NN-*.md fayllarini bitta docs/TOLIQ-DOKUMENTATSIYA.md ga yig'adi.

Ishlatish (loyiha ildizidan):  python docs/build-toliq.py

- Fayllar raqam tartibida birlashtiriladi.
- Har bir bo'limning yuqori/pastki navigatsiya qatorlari ("← ... · Keyingisi: ... →") olib tashlanadi.
- Fayllararo havolalar (`05-malumotlar-bazasi.md#x`) ichki anchor'larga (`#x`) aylantiriladi;
  anchor'siz fayl havolasi o'sha bo'lim sarlavhasiga yo'naltiriladi.
"""
import io
import re
from pathlib import Path

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "TOLIQ-DOKUMENTATSIYA.md"
FILES = sorted(p for p in DOCS.glob("[0-9][0-9]-*.md"))
LINK = re.compile(r"\]\((\d\d-[\w-]+\.md)(#[^)]*)?\)")
NAV = re.compile(r"^\s*(\[←|Keyingisi:|\[Hujjatlar ro'yxatiga)")


def anchor(heading):
    """GitHub uslubidagi anchor: kichik harf, tinish belgilarsiz, bo'shliq -> '-'."""
    return re.sub(r"[^\w\- ]", "", heading.strip().lower()).replace(" ", "-")


def main():
    titles = {}
    for p in FILES:
        first = io.open(p, encoding="utf-8").readline()
        titles[p.name] = first.lstrip("#").strip()

    def relink(m):
        target, frag = m.group(1), m.group(2)
        if frag:
            return "](" + frag + ")"
        if target in titles:
            return "](#" + anchor(titles[target]) + ")"
        return m.group(0)

    parts = [
        "# Maktab Boshqaruv — to'liq dokumentatsiya\n",
        "> Bu fayl avtomatik yig'ilgan (`python docs/build-toliq.py`) — uni qo'lda tahrirlamang, "
        "`docs/NN-*.md` fayllarini o'zgartirib, qayta yig'ing.\n",
        "## Mundarija\n",
    ]
    parts.append("\n".join(f"- [{t}](#{anchor(t)})" for t in titles.values()) + "\n")

    for p in FILES:
        lines = io.open(p, encoding="utf-8").read().rstrip("\n").split("\n")
        lines = [ln for ln in lines if not NAV.match(ln)]
        while lines and lines[-1].strip() in ("", "---"):
            lines.pop()
        body = "\n".join(lines)
        parts.append("\n---\n\n" + LINK.sub(relink, body) + "\n")

    io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(parts))
    print(f"{OUT.name}: {len(FILES)} ta bo'lim yig'ildi")


if __name__ == "__main__":
    main()
