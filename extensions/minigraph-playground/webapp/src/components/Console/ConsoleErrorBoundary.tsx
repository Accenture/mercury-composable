import React from 'react';

interface ConsoleErrorBoundaryProps {
  fallback: string;
  children: React.ReactNode;
}

interface ConsoleErrorBoundaryState {
  hasError: boolean;
}

export class ConsoleErrorBoundary extends React.Component<
  ConsoleErrorBoundaryProps,
  ConsoleErrorBoundaryState
> {
  state: ConsoleErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ConsoleErrorBoundaryState {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return <span>{this.props.fallback}</span>;
    }
    return this.props.children;
  }
}
