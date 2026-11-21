import { MoreHorizontal, Edit3, Trash2, Flag, Copy, EyeOff } from "lucide-react";
import { useState } from "react";

const items = [
  { label: "Edit", icon: Edit3 },
  { label: "Delete", icon: Trash2 },
  { label: "Report", icon: Flag },
  { label: "Copy Link", icon: Copy },
  { label: "Hide", icon: EyeOff },
];

export default function PostMenu() {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-800 hover:text-white"
      >
        <MoreHorizontal className="h-5 w-5" />
      </button>

      {open && (
        <div className="absolute right-0 z-10 mt-2 w-44 rounded-xl border border-slate-800 bg-slate-950 p-2 shadow-2xl">
          {items.map(({ label, icon: Icon }) => (
            <button
              key={label}
              type="button"
              className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800 hover:text-white"
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
