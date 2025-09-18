import type {
  HybridView,
  HybridViewMethods,
  HybridViewProps,
} from 'react-native-nitro-modules';

export interface NitroTimerProps extends HybridViewProps {
  color: string;
}
export interface NitroTimerMethods extends HybridViewMethods {}

export type NitroTimer = HybridView<
  NitroTimerProps,
  NitroTimerMethods
>;
