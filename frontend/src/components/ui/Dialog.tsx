import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";

interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
}

export default function Dialog({
  open,
  onOpenChange,
  title,
  children,
}: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm" />

        <DialogPrimitive.Content
          className="
            fixed
            left-1/2
            top-1/2
            z-50
            max-h-[90vh]
            w-[92vw]
            max-w-2xl
            -translate-x-1/2
            -translate-y-1/2
            overflow-y-auto
            rounded-2xl
            border
            border-slate-800
            bg-slate-900
            p-4
            shadow-2xl
            outline-none
            sm:p-6
          "
        >
          <div className="mb-4 flex items-start justify-between gap-3 sm:mb-5">
            <DialogPrimitive.Title className="text-lg font-semibold text-white">
              {title}
            </DialogPrimitive.Title>

            <DialogPrimitive.Close className="rounded-full p-2 text-slate-400 transition hover:bg-slate-800 hover:text-white">
              <X className="h-5 w-5" />
            </DialogPrimitive.Close>
          </div>

          {children}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}