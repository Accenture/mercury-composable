import { useEffect, useRef } from 'react';
import { type ProtocolBus } from '../protocol/bus';
import { type HelpContentProfile } from '../data/helpContent';
import { resolveBundledHelpTopic } from '../utils/localHelpCommand';

export interface UseAutoHelpNavigateOptions {
  bus:          ProtocolBus;
  /** Called with the extracted topic key (empty string = root index). */
  setHelpTopic: (topic: string) => void;
  /** Called to open the help panel (a separate third resizable panel). */
  onTabSwitch:  () => void;
  /** Disables local help navigation for playgrounds without a Help profile. */
  enabled?:      boolean;
  /** Selects the locally bundled content set for this playground. */
  contentProfile?: HelpContentProfile;
}

/**
 * Subscribes to `command.helpOrDescribe` events on the ProtocolBus.
 * When the command shows a help page — `help {topic}`, or `describe skill
 * {route}` for a built-in skill — sets that topic and opens the help panel,
 * but only when the page is bundled locally.  For non-bundled topics the
 * server response shown in the console is the only copy, so the panel is
 * left closed.
 *
 * The other `describe` commands (node, connection) answer in the console, and
 * `describe graph` is never classified here (it belongs to the Graph tab).
 */
export function useAutoHelpNavigate({
  bus,
  setHelpTopic,
  onTabSwitch,
  enabled = true,
  contentProfile = 'minigraph',
}: UseAutoHelpNavigateOptions): void {
  // Use a ref for onTabSwitch to avoid re-subscribing on every render if the
  // caller passes an inline arrow function.
  const onTabSwitchRef = useRef(onTabSwitch);
  useEffect(() => { onTabSwitchRef.current = onTabSwitch; });

  useEffect(() => {
    if (!enabled) return;
    return bus.on('command.helpOrDescribe', (event) => {
      // The same resolver decides which typed commands are handled locally,
      // so a locally echoed command and a server-echoed one (a collaborator's
      // or the companion's) open the same page.  Opening a panel to show
      // "not found" is low-value UX, hence the bundled-content check inside.
      const topic = resolveBundledHelpTopic(event.commandText, true, contentProfile);
      if (topic === null) return;

      setHelpTopic(topic);
      onTabSwitchRef.current();
    });
  }, [bus, setHelpTopic, enabled, contentProfile]);
}
