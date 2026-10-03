import type { SVGProps } from 'react';

type ToolbarIconProps = Omit<SVGProps<SVGSVGElement>, 'children'>;

const sharedProps = {
  viewBox: '0 0 16 16',
  width: 16,
  height: 16,
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  focusable: 'false',
} as const;

export function InstantiateIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M8 1.75 13.25 4.75v6.5L8 14.25l-5.25-3v-6.5L8 1.75Z" />
      <path d="m2.95 4.9 5.05 2.9 5.05-2.9M8 7.8v6.15" />
    </svg>
  );
}

export function RunIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="m5.25 3 6.5 5-6.5 5V3Z" />
    </svg>
  );
}

export function CopyIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <rect x="5" y="2" width="8.5" height="9" rx="1.25" />
      <path d="M10.75 14h-7A1.25 1.25 0 0 1 2.5 12.75V6A1.25 1.25 0 0 1 3.75 4.75H5" />
    </svg>
  );
}

export function UploadIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M8 10.5V2.75M4.75 6 8 2.75 11.25 6" />
      <path d="M2.75 10.75v1.5a1 1 0 0 0 1 1h8.5a1 1 0 0 0 1-1v-1.5" />
    </svg>
  );
}

export function DownloadIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M8 2.75v7.75M4.75 7.25 8 10.5l3.25-3.25" />
      <path d="M2.75 10.75v1.5a1 1 0 0 0 1 1h8.5a1 1 0 0 0 1-1v-1.5" />
    </svg>
  );
}

export function ImportIcon(props: ToolbarIconProps) {
  return (
    <svg {...sharedProps} {...props}>
      <path d="M2.75 8h7M7 5.25 9.75 8 7 10.75" />
      <path d="M8.75 2.75h3.5a1 1 0 0 1 1 1v8.5a1 1 0 0 1-1 1h-3.5" />
    </svg>
  );
}
