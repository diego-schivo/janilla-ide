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

export default class JanillaIde extends FlexibleElement {

	static get templateName() {
		return "janilla-ide";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("JanillaIde.connectedCallback");
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		// console.log("JanillaIde.disconnectedCallback");
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleSubmit = async event => {
		// console.log("JanillaIde.handleSubmit", event);
		event.preventDefault();
		event.stopPropagation();
		const a = event.submitter.formAction ?? event.target.action;
		const m = event.submitter.formMethod ?? event.target.method;
		if (m === "get") {
			this.projects[0].entries = await (await fetch([a, new URLSearchParams(new FormData(event.target))].join("?"))).json();
			this.requestUpdate();
			return;
		}

		const j = await (await fetch(a, {
			method: m,
			headers: { "content-type": "application/json" },
			body: JSON.stringify(Object.fromEntries(new FormData(event.target)))
		})).json();
		console.log("JanillaIde.handleSubmit", j);

		if (j) {
			const es = new EventSource("/api/projects/output");
			es.onopen = () => {
				console.log("Connection to server opened.");
			};
			es.onmessage = async e => {
				console.log(e.data);
				if (e.data === "ok") {
					es.close();
					this.requestUpdate();
				}
			};
			es.onerror = () => {
				console.log("EventSource failed.");
			};
		}
	}

	async updateDisplay() {
		// console.log("JanillaIde.updateDisplay");
		this.projects ??= await (await fetch("/api/projects")).json();
		this.appendChild(this.interpolateDom({
			$template: "",
			items: this.projects?.map(x => ({
				$template: "item",
				...x,
				content: x.entries?.length ? {
					$template: "content",
					entries: x.entries.map(y => ({
						$template: "entry",
						name: y
					}))
				} : null
			}))
		}));
	}
}
