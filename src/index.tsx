import { getHostComponent } from 'react-native-nitro-modules';
const NitroTimerConfig = require('../nitrogen/generated/shared/json/NitroTimerConfig.json');
import type {
  TimerData,
  NitroTimerMethods,
  NitroTimerProps,
  NitroTimer,
} from './NitroTimer.nitro';

export const NitroTimerView = getHostComponent<
  NitroTimerProps,
  NitroTimerMethods
>('NitroTimer', () => NitroTimerConfig);

// Export types for external use
export type { TimerData, NitroTimerProps, NitroTimerMethods, NitroTimer };
