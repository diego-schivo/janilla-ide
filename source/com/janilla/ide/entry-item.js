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
import { FlexibleElement } from "./flexible-element.js";

export default class EntryItem extends FlexibleElement {

	static get templateName() {
		return "entry-item";
	}

	get state() {
		let i = 0;
		for (let el = this.parentElement.firstElementChild; el !== this; el = el.nextElementSibling)
			i++;
		return this.closest("entry-list").state.items[i];
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		// console.log("EntryItem.connectedCallback");
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		// console.log("EntryItem.disconnectedCallback");
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		// console.log("EntryItem.handleClick", event);
		const a = event.composedPath().find(x => x.tagName?.toLowerCase() === "a");
		if (a?.href) {
			event.preventDefault();
			event.stopPropagation();
			this.state.view = parseInt(a.textContent);
			this.requestUpdate();
		}
	}

	async updateDisplay() {
		// console.log("EntryItem.updateDisplay");
		const s = this.state;
		this.shadowRoot.appendChild(this.interpolateDom({
			$template: "shadow",
			items: s.views.map((x, i) => ({
				$template: "shadow-item",
				...x,
				class: i === s.view ? "active" : null,
				name: i.toString()
			}))
		}));
		this.appendChild(this.interpolateDom({
			$template: "",
			views: s.views.map((x, i) => ({
				$template: "view",
				...x,
				slot: i === s.view ? "content" : null
			}))
		}));
	}
}
