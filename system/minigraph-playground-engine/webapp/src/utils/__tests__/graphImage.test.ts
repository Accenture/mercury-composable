// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const htmlToImage = vi.hoisted(() => ({
  toBlob: vi.fn(),
  toSvg: vi.fn(),
}));

vi.mock('html-to-image', () => htmlToImage);

import {
  GRAPH_IMAGE_FALLBACK_BACKGROUND,
  GRAPH_IMAGE_PADDING,
  GRAPH_IMAGE_PIXEL_RATIO,
  edgeBounds,
  graphImageFileName,
  graphImageFrame,
  includeInGraphImage,
  renderGraphImage,
  resolveBackgroundColor,
  unionRects,
  withSolidBackground,
} from '../graphImage';

const SVG_NS = 'http://www.w3.org/2000/svg';

afterEach(() => {
  document.body.innerHTML = '';
  vi.clearAllMocks();
});

describe('graphImageFileName', () => {
  it('names the file after the graph id that Download suggests', () => {
    expect(graphImageFileName('My Graph', 'png')).toBe('My-Graph.png');
    expect(graphImageFileName('tutorial-12', 'svg')).toBe('tutorial-12.svg');
    expect(graphImageFileName(undefined, 'png')).toBe('untitled.png');
  });
});

describe('unionRects', () => {
  it('answers null for no rectangles', () => {
    expect(unionRects([])).toBeNull();
  });

  it('spans every rectangle, negative coordinates included', () => {
    expect(unionRects([
      { x: 0, y: -50, width: 240, height: 100 },
      { x: 360, y: 20, width: 240, height: 140 },
      { x: -30, y: 10, width: 10, height: 10 },
    ])).toEqual({ x: -30, y: -50, width: 630, height: 210 });
  });
});

describe('graphImageFrame', () => {
  it('draws the bounds at zoom 1 with the padding on every side', () => {
    // 600 x 210 graph pixels + 2 x 40 padding = 680 x 290; the origin moves to (40 - x, 40 - y).
    expect(graphImageFrame({ x: -30, y: -50, width: 600, height: 210 })).toEqual({
      width: 680,
      height: 290,
      transform: 'translate(70px, 90px) scale(1)',
    });
    expect(GRAPH_IMAGE_PADDING).toBe(40);
  });

  it('rounds a fractional size up to whole pixels', () => {
    const frame = graphImageFrame({ x: 0, y: 0, width: 100.2, height: 50.6 }, 10);
    expect(frame.width).toBe(121);
    expect(frame.height).toBe(71);
  });
});

describe('includeInGraphImage', () => {
  function element(className: string): HTMLElement {
    const div = document.createElement('div');
    div.className = className;
    return div;
  }

  it('leaves out handles, resize controls and the box-selection rectangle', () => {
    expect(includeInGraphImage(element('react-flow__handle react-flow__handle-left'))).toBe(false);
    expect(includeInGraphImage(element('react-flow__resize-control handle bottom right'))).toBe(false);
    expect(includeInGraphImage(element('react-flow__nodesselection react-flow__container'))).toBe(false);
  });

  it('keeps nodes, edges and text', () => {
    expect(includeInGraphImage(element('react-flow__node react-flow__node-Root selected'))).toBe(true);
    expect(includeInGraphImage(element('react-flow__edge selected'))).toBe(true);
    expect(includeInGraphImage(document.createTextNode('root') as unknown as HTMLElement)).toBe(true);
  });
});

describe('edgeBounds', () => {
  it('reads every edge group with getBBox and skips empty boxes', () => {
    const viewport = document.createElement('div');
    const boxes = [
      { x: -60, y: 10, width: 120, height: 40 },
      { x: 0, y: 0, width: 0, height: 0 },
    ];
    for (const box of boxes) {
      const group = document.createElementNS(SVG_NS, 'g');
      group.setAttribute('class', 'react-flow__edge');
      (group as unknown as { getBBox: () => typeof box }).getBBox = () => box;
      viewport.appendChild(group);
    }

    expect(edgeBounds(viewport)).toEqual([{ x: -60, y: 10, width: 120, height: 40 }]);
  });

  it('answers nothing where the browser has no getBBox', () => {
    const viewport = document.createElement('div');
    const group = document.createElementNS(SVG_NS, 'g');
    group.setAttribute('class', 'react-flow__edge');
    viewport.appendChild(group);
    (group as unknown as { getBBox?: unknown }).getBBox = undefined;

    expect(edgeBounds(viewport)).toEqual([]);
  });
});

describe('resolveBackgroundColor', () => {
  it('takes the first opaque background of the element or an ancestor', () => {
    const wrapper = document.createElement('div');
    wrapper.style.backgroundColor = 'rgb(255, 255, 255)';
    const surface = document.createElement('div');
    const viewport = document.createElement('div');
    surface.appendChild(viewport);
    wrapper.appendChild(surface);
    document.body.appendChild(wrapper);

    expect(resolveBackgroundColor(viewport)).toBe('rgb(255, 255, 255)');
  });

  it('falls back to white when nothing has a background', () => {
    const viewport = document.createElement('div');
    document.body.appendChild(viewport);

    expect(resolveBackgroundColor(viewport)).toBe(GRAPH_IMAGE_FALLBACK_BACKGROUND);
    expect(resolveBackgroundColor(null)).toBe(GRAPH_IMAGE_FALLBACK_BACKGROUND);
  });
});

describe('withSolidBackground', () => {
  it('puts a full-size rectangle of the colour behind the content', () => {
    const svg = `<svg xmlns="${SVG_NS}" width="680" height="290" viewBox="0 0 680 290">`
      + '<foreignObject width="100%" height="100%" x="0" y="0"></foreignObject></svg>';

    const doc = new DOMParser().parseFromString(withSolidBackground(svg, 'rgb(255, 255, 255)'), 'image/svg+xml');
    const children = Array.from(doc.documentElement.children);

    expect(children.map((child) => child.tagName)).toEqual(['rect', 'foreignObject']);
    expect(children[0].getAttribute('width')).toBe('100%');
    expect(children[0].getAttribute('height')).toBe('100%');
    expect(children[0].getAttribute('fill')).toBe('rgb(255, 255, 255)');
    expect(doc.documentElement.getAttribute('width')).toBe('680');
  });
});

describe('renderGraphImage', () => {
  const bounds = { x: -30, y: -50, width: 600, height: 210 };
  let viewport: HTMLElement;

  beforeEach(() => {
    viewport = document.createElement('div');
    viewport.className = 'react-flow__viewport';
  });

  it('renders a PNG of the framed graph at twice the graph size', async () => {
    const png = new Blob(['png'], { type: 'image/png' });
    htmlToImage.toBlob.mockResolvedValue(png);

    const result = await renderGraphImage(viewport, bounds, 'png', 'rgb(255, 255, 255)');

    expect(result).toBe(png);
    expect(htmlToImage.toBlob).toHaveBeenCalledTimes(1);
    const [target, options] = htmlToImage.toBlob.mock.calls[0];
    expect(target).toBe(viewport);
    expect(options).toMatchObject({
      backgroundColor: 'rgb(255, 255, 255)',
      width: 680,
      height: 290,
      pixelRatio: GRAPH_IMAGE_PIXEL_RATIO,
      style: { width: '680px', height: '290px', transform: 'translate(70px, 90px) scale(1)' },
    });
    expect(options.filter).toBe(includeInGraphImage);
  });

  it('fails by name when the browser draws no PNG', async () => {
    htmlToImage.toBlob.mockResolvedValue(null);

    await expect(renderGraphImage(viewport, bounds, 'png', 'white')).rejects.toThrow('The browser could not draw the image.');
  });

  it('renders an SVG file with a solid background rectangle', async () => {
    const svg = `<svg xmlns="${SVG_NS}" width="680" height="290" viewBox="0 0 680 290">`
      + '<foreignObject width="100%" height="100%"><div xmlns="http://www.w3.org/1999/xhtml">root ✓</div></foreignObject></svg>';
    htmlToImage.toSvg.mockResolvedValue(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`);

    const result = await renderGraphImage(viewport, bounds, 'svg', 'rgb(255, 255, 255)');

    expect(result.type).toBe('image/svg+xml');
    const text = await result.text();
    expect(text.startsWith('<svg')).toBe(true);
    expect(text).toContain('<rect width="100%" height="100%" fill="rgb(255, 255, 255)"');
    expect(text).toContain('root ✓');
    expect(htmlToImage.toSvg.mock.calls[0][1]).not.toHaveProperty('pixelRatio');
    expect(htmlToImage.toBlob).not.toHaveBeenCalled();
  });
});
