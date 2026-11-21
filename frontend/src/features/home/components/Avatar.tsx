import { useState } from "react";
import { UserRound } from "lucide-react";
import { resolveImageUrl } from "../../../utils/image";

interface AvatarProps {
  imageUrl?: string | null;
  name: string;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
}

const sizeClasses: Record<NonNullable<AvatarProps["size"]>, string> = {
  sm: "h-8 w-8",
  md: "h-9 w-9",
  lg: "h-10 w-10",
  xl: "h-12 w-12",
};

export default function Avatar({
  imageUrl,
  name,
  size = "md",
  className = "",
}: AvatarProps) {
  const resolvedImageUrl = resolveImageUrl(imageUrl);
  const [failedImageUrl, setFailedImageUrl] = useState<string>();
  const avatarClasses = `rounded-full ring-2 ring-slate-700 ${sizeClasses[size]} ${className}`;

  return resolvedImageUrl && resolvedImageUrl !== failedImageUrl ? (
    <img
      src={resolvedImageUrl}
      alt={name}
      className={`object-cover ${avatarClasses}`}
      onError={() => setFailedImageUrl(resolvedImageUrl)}
    />
  ) : (
    <div
      role="img"
      aria-label={`${name} avatar`}
      className={`flex items-center justify-center bg-muted text-muted-foreground ${avatarClasses}`}
    >
      <UserRound aria-hidden="true" className="size-1/2" strokeWidth={2.5} />
    </div>
  );
}
