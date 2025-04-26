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

export default class EntryList extends WebComponent {

	static get templateName() {
		return "entry-list";
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		super.connectedCallback();
		this.shadowRoot.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.shadowRoot.removeEventListener("click", this.handleClick);
	}

	handleClick = event => {
		const li = event.target.closest("li");
		if (li?.closest("ul") !== this.shadowRoot.querySelector("ul"))
			return;
		const a = li.querySelector("a");
		const p = new URL(a.href).pathname.substring("/entry/".length);
		if (event.target.closest("a")) {
			event.preventDefault();
			this.dispatchEvent(new CustomEvent("select-entry", {
				bubbles: true,
				detail: { path: p }
			}));
		} else
			this.dispatchEvent(new CustomEvent("close-entry", {
				bubbles: true,
				detail: { path: p }
			}));
	}

	async updateDisplay() {
		const s = this.closest("root-layout").state;
		const df = this.interpolateDom({
			$template: "",
			items: s.paths.map((x, i) => ({
				$template: "item",
				path: x,
				name: x ? x.split("/").at(-1) : "-",
				active: i === s.activeIndex ? "active" : null
			})),
			articles: s.paths.map((x, i) => ({
				$template: "article",
				slot: i === s.activeIndex ? "content" : null,
				path: x
			}))
		});
		this.shadowRoot.append(...df.querySelectorAll("link, ul, slot"));
		this.appendChild(df);
	}
}
