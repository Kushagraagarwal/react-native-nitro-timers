import type {
  HybridView,
  HybridViewMethods,
  HybridViewProps,
} from 'react-native-nitro-modules';

export interface TimerData {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  totalSeconds: number;
  isExpired: boolean;
  isCritical: boolean; // When < criticalThreshold
}

export interface NitroTimerProps extends HybridViewProps {
  // Core timer config
  endTime: string; // ISO 8601 format
  autoStart: boolean;
  criticalThreshold: number; // Seconds when timer becomes "critical"

  // Display format
  format: 'HH:MM:SS' | 'H:M:S' | 'Hh Mm Ss' | 'H hours M minutes';
  showDays: boolean;
  hideZeroHours: boolean;

  // Styling
  fontSize: number;
  textColor: string;
  criticalColor: string; // Color when isCritical = true
  fontWeight: 'normal' | 'bold';
  textAlign: 'left' | 'center' | 'right';
  backgroundColor?: string;
  cornerRadius?: number;
  padding?: number;

  // Behavior
  pauseWhenNotVisible: boolean;
  continueInBackground: boolean;

  // Events (minimal bridge usage)
  onExpired?: () => void;
  onCritical?: () => void;
}

export interface NitroTimerMethods extends HybridViewMethods {
  start(): void;
  pause(): void;
  resume(): void;
  reset(): void;
  addSeconds(seconds: number): void;
  setEndTime(endTime: string): void;
  getCurrentTime(): Promise<TimerData>;
}

export type NitroTimer = HybridView<
  NitroTimerProps,
  NitroTimerMethods
>;
