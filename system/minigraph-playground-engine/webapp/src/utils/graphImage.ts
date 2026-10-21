import { toBlob } from 'html-to-image';
import { suggestGraphId } from './graphFile';

/**
 * Image export of the Graph tab: the whole graph as a PNG file, whatever part of it the viewport
 * shows. `html-to-image` (React Flow's documented approach) copies the React Flow viewport element
 * with every element's computed style inlined, wraps the copy in an SVG `foreignObject` and draws
 * that on a canvas. The viewport holds only the edges and the nodes - the minimap, the zoom
 * controls and the dotted background are its siblings, so they never appear.
 *
 * PNG only, by design: the intermediate SVG keeps the node cards as HTML inside `foreignObject`,
 * which browsers draw but most other SVG renderers (vector editors, office suites, librsvg) show
 * as a blank image, so an SVG file would fail exactly where an exported graph is used.
 */

/** A rectangle in graph coordinates (React Flow's `Rect`, the unit of node positions). */
export interface GraphRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Space around the graph in the image, in graph pixels; it also holds node shadows and edge strokes. */
export const GRAPH_IMAGE_PADDING = 40;

/**
 * PNG pixels per graph pixel. 2 keeps text sharp on high-density screens and in print, whatever the
 * screen the export ran on. `html-to-image` scales a canvas side above 16,384 pixels down to fit.
 */
export const GRAPH_IMAGE_PIXEL_RATIO = 2;

/** The background when no ancestor of the canvas has one: white, the canvas colour (`--bg-primary`). */
export const GRAPH_IMAGE_FALLBACK_BACKGROUND = '#ffffff';

/**
 * Elements left out of the image, by React Flow class: the connection handles (including the
 * authoring halo drawn around a selected node), the resize frame of a selected node and the
 * rectangle around a box selection. They are editing affordances, not part of the graph.
 */
const EXCLUDED_CLASSES = ['react-flow__handle', 'react-flow__resize-control', 'react-flow__nodesselection'];

/** `<graph-id>.png`, the graph id derived from the display name as Download does. */
export function graphImageFileName(graphName: string | null | undefined): string {
  return `${suggestGraphId(graphName)}.png`;
}

/** The smallest rectangle holding every given one; null for none. */
export function unionRects(rects: readonly GraphRect[]): GraphRect | null {
  if (rects.length === 0) return null;
  let left = Infinity;
  let top = Infinity;
  let right = -Infinity;
  let bottom = -Infinity;
  for (const rect of rects) {
    left = Math.min(left, rect.x);
    top = Math.min(top, rect.y);
    right = Math.max(right, rect.x + rect.width);
    bottom = Math.max(bottom, rect.y + rect.height);
  }
  return { x: left, y: top, width: right - left, height: bottom - top };
}

/** The image size, in graph pixels, and the viewport transform that draws `bounds` at zoom 1, `padding` from each edge. */
export interface GraphImageFrame {
  width: number;
  height: number;
  transform: string;
}

/**
 * Frames the whole graph at zoom 1: one graph pixel is one image pixel (the PNG multiplies by
 * {@link GRAPH_IMAGE_PIXEL_RATIO}), so the image is as large as the graph and nothing is cut off.
 * The size is rounded up to whole pixels.
 */
export function graphImageFrame(bounds: GraphRect, padding: number = GRAPH_IMAGE_PADDING): GraphImageFrame {
  return {
    width: Math.ceil(bounds.width + 2 * padding),
    height: Math.ceil(bounds.height + 2 * padding),
    transform: `translate(${padding - bounds.x}px, ${padding - bounds.y}px) scale(1)`,
  };
}

/** `html-to-image`'s filter: false for an element left out of the image (and its children). */
export function includeInGraphImage(element: HTMLElement): boolean {
  const classList = element.classList;
  if (!classList) return true; // text nodes
  return !EXCLUDED_CLASSES.some((name) => classList.contains(name));
}

/**
 * The bounds of the rendered edges in graph coordinates. Every edge is an SVG group in a layer at
 * the viewport's origin, so `getBBox()` answers in graph coordinates. A back edge curls outside its
 * nodes, and the node bounds alone would cut the curl off.
 */
export function edgeBounds(viewport: Element): GraphRect[] {
  const rects: GraphRect[] = [];
  for (const edge of Array.from(viewport.querySelectorAll('.react-flow__edge'))) {
    const graphics = edge as SVGGraphicsElement;
    if (typeof graphics.getBBox !== 'function') continue;
    const box = graphics.getBBox();
    if (box.width > 0 || box.height > 0) {
      rects.push({ x: box.x, y: box.y, width: box.width, height: box.height });
    }
  }
  return rects;
}

function isTransparent(color: string): boolean {
  return color === '' || color === 'transparent' || /^rgba\(.*,\s*0\)$/.test(color);
}

/** The colour the canvas shows behind the graph: the first opaque background of the element or an ancestor. */
export function resolveBackgroundColor(element: Element | null): string {
  for (let current = element; current; current = current.parentElement) {
    const color = getComputedStyle(current).backgroundColor;
    if (!isTransparent(color)) return color;
  }
  return GRAPH_IMAGE_FALLBACK_BACKGROUND;
}

/**
 * Renders the graph to a PNG. `viewport` is React Flow's `.react-flow__viewport` element and
 * `bounds` the graph's extent in graph coordinates (nodes and edges). `html-to-image` fills the
 * whole canvas with `backgroundColor` first, so every pixel is opaque. The caller hides selection
 * styling while this runs, because `html-to-image` copies the computed styles of the live elements.
 */
export async function renderGraphImage(
  viewport: HTMLElement,
  bounds: GraphRect,
  backgroundColor: string,
): Promise<Blob> {
  const frame = graphImageFrame(bounds);
  const blob = await toBlob(viewport, {
    backgroundColor,
    width: frame.width,
    height: frame.height,
    pixelRatio: GRAPH_IMAGE_PIXEL_RATIO,
    style: {
      width: `${frame.width}px`,
      height: `${frame.height}px`,
      transform: frame.transform,
    },
    filter: includeInGraphImage,
  });
  if (!blob) throw new Error('The browser could not draw the image.');
  return blob;
}
