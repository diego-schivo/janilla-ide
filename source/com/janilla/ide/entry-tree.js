/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import { WebComponent } from "./web-component.js";

export default class EntryTree extends WebComponent {

	static get templateName() {
		return "entry-tree";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		const li = event.target.closest("li");
		if (!li) {
			this.dispatchEvent(new CustomEvent("select-entry", {
				bubbles: true,
				detail: { path: null }
			}));
			return;
		}
		const a = li.querySelector("a");
		const p = new URL(a.href).pathname.substring("/entry/".length);
		if (event.target.closest("a")) {
			event.preventDefault();
			this.dispatchEvent(new CustomEvent("select-entry", {
				bubbles: true,
				detail: { path: p }
			}));
		} else if (a.classList.contains("directory")) {
			const t = a.classList.contains("expanded") ? "collapse-directory" : "expand-directory";
			this.dispatchEvent(new CustomEvent(t, {
				bubbles: true,
				detail: { path: p }
			}));
		}
	}

	async updateDisplay() {
		const s = this.closest("root-layout").state;
		const p = s.activeIndex !== -1 ? s.paths[s.activeIndex] : null;
		const c = (entries, path) => entries ? {
			$template: "list",
			items: entries.map(x => {
				const p2 = path ? `${path}/${x.name}` : x.name;
				return {
					$template: "item",
					path: p2,
					directory: Object.hasOwn(x, "entries") ? "directory" : null,
					expanded: x.entries ? "expanded" : null,
					active: p2 === p ? "active" : null,
					...x,
					content: c(x.entries, p2)
				};
			})
		} : null;
		this.appendChild(this.interpolateDom({
			$template: "",
			content: c(s.entries)
		}));
	}
}
