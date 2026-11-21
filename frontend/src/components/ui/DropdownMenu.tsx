import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";

type DropdownMenuProps = {
  label: string;
  trigger: ReactNode;
  children: (api: { close: () => void }) => ReactNode;
};

export function DropdownMenu({ label, trigger, children }: DropdownMenuProps) {
  const [open, setOpen] = useState(false);
  const [menuStyle, setMenuStyle] = useState<CSSProperties>({});
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const close = () => setOpen(false);
  const toggle = () => setOpen((current) => !current);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        open &&
        menuRef.current &&
        triggerRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        !triggerRef.current.contains(event.target as Node)
      ) {
        close();
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        close();
      }
    };

    const updateMenuPosition = () => {
      if (!triggerRef.current || !menuRef.current) {
        return;
      }

      const rect = triggerRef.current.getBoundingClientRect();
      const menuWidth = 208;
      const menuHeight = 165;
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;

      const left = rect.right - 50;
      const top = rect.top;

      const nextLeft = left + menuWidth > viewportWidth ? rect.left - menuWidth - 12 : left;
      const nextTop = top + menuHeight > viewportHeight ? rect.top - menuHeight - 12 : top;

      setMenuStyle({
        left: nextLeft,
        top: nextTop,
        position: "fixed",
      });
    };

    if (open) {
      updateMenuPosition();
      window.addEventListener("resize", updateMenuPosition);
      window.addEventListener("scroll", updateMenuPosition, true);
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);

    return () => {
      window.removeEventListener("resize", updateMenuPosition);
      window.removeEventListener("scroll", updateMenuPosition, true);
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  return (
    <div className="relative">
      <button
        type="button"
        ref={triggerRef}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={label}
        onClick={toggle}
        className="flex h-12 w-12 items-center justify-center rounded-full border border-slate-800 bg-slate-950/90 text-slate-300 shadow-sm transition-all duration-200 ease-out hover:-translate-y-0.5 hover:border-slate-700 hover:bg-slate-900 hover:text-white focus:outline-none focus:ring-2 focus:ring-fuchsia-500/60 active:scale-[0.98]"
      >
        {trigger}
      </button>

      <div
        ref={menuRef}
        role="menu"
        style={menuStyle}
        className={`fixed z-[60] min-w-[13rem] overflow-hidden rounded-3xl border border-white/10 bg-slate-950/95 p-2 shadow-2xl shadow-black/40 backdrop-blur transition-all duration-200 ease-out ${
          open
            ? "pointer-events-auto scale-100 opacity-100"
            : "pointer-events-none scale-95 opacity-0"
        }`}
      >
        {children({ close })}
      </div>
    </div>
  );
}
