interface CharacterCounterProps {
  current: number;
  max: number;
}

export default function CharacterCounter({
  current,
  max,
}: CharacterCounterProps) {
  const percentage = Math.min((current / max) * 100, 100);

  const radius = 12;
  const circumference = 2 * Math.PI * radius;

  const offset =
    circumference - (percentage / 100) * circumference;

  const color =
    current >= max
      ? "#ef4444"
      : current >= max * 0.8
      ? "#f59e0b"
      : "#38bdf8";

  return (
    <div className="relative h-8 w-8">
      <svg className="h-8 w-8 -rotate-90">
        <circle
          cx="16"
          cy="16"
          r={radius}
          stroke="#334155"
          strokeWidth="3"
          fill="none"
        />

        <circle
          cx="16"
          cy="16"
          r={radius}
          stroke={color}
          strokeWidth="3"
          fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
        />
      </svg>

      {max - current <= 20 && (
        <span className="absolute inset-0 flex items-center justify-center text-[10px] font-medium text-slate-300">
          {max - current}
        </span>
      )}
    </div>
  );
}