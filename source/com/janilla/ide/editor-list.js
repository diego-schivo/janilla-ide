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

export default class EditorList extends WebComponent {

	static get observedAttributes() {
		return ["data-path"];
	}

	static get templateName() {
		return "editor-list";
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		super.connectedCallback();
		this.state.activeIndex = 0;
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = event => {
		const a = event.composedPath().find(x => x.tagName?.toLowerCase() === "a");
		const ul = this.shadowRoot.querySelector("ul");
		if (a?.closest("ul") !== ul)
			return;
		event.preventDefault();
		event.stopPropagation();
		this.state.activeIndex = Array.prototype.indexOf.call(ul.children, a.closest("li"));
		this.requestDisplay();
	}

	async updateDisplay() {
		const p = this.dataset.path ?? null;
		const s = this.state;
		if (p !== s.path) {
			const df = this.interpolateDom({ $template: "" });
			this.shadowRoot.append(...df.querySelectorAll("link, slot, ul"));
			this.appendChild(df);
			s.path = p;
			s.entry = await (await fetch(`/api/entries/${p ?? ""}`)).json();
		}
		const nn = s.entry.entries ? ["directory-content"] : ["file-content"];
		const df = this.interpolateDom({
			$template: "",
			items: nn.map((x, i) => ({
				$template: "item",
				name: x,
				active: i === s.activeIndex ? "active" : null
			})),
			articles: nn.map((x, i) => ({
				$template: `article-${x}`,
				slot: i === s.activeIndex ? "content" : null
			}))
		});
		this.shadowRoot.append(...df.querySelectorAll("link, slot, ul"));
		this.appendChild(df);
	}
}
