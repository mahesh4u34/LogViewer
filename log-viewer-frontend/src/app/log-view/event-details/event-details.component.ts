import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnChanges,
    QueryList,
    SimpleChanges,
    ViewChildren
} from '@angular/core';
import {ViewConfigService} from '../view-config.service';
import {Record} from '@app/log-view/record';
import {LogFile} from '@app/log-view/log-file';
import {ViewStateService} from '@app/log-view/view-state.service';
import {LvUtils} from '@app/utils/utils';
import {RecordRendererService} from '@app/log-view/record-renderer.service';

@Component({
    selector: 'lv-event-details',
    templateUrl: './event-details.template.html',
    styleUrls: ['./event-details.style.scss']
})
export class EventDetailsComponent implements OnChanges, AfterViewInit {

    @ViewChildren('fieldVal') fieldValues: QueryList<ElementRef>;

    @Input() record: Record;

    log: LogFile;

    timestamp: string;

    fields: {fieldName: string, html: HTMLElement}[];

    constructor(private changeDetectorRef: ChangeDetectorRef,
                public vs: ViewStateService,
                private recRenderer: RecordRendererService,
                private viewConfig: ViewConfigService) {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['record']) {
            let r = this.record;

            this.log = this.viewConfig.logById[r.logId];

            this.timestamp = r.time ? LvUtils.formatDate(LvUtils.nano2milliseconds(r.time)) : null;

            this.fields = [];

            if (this.log) {
                let fieldsCopy = [...r.fields];
                fieldsCopy.sort((a, b) => a.start - b.start);

                for (let field of fieldsCopy) {
                    let e: HTMLElement = document.createElement('SPAN');
                    e.className = 'rec-text';

                    let fieldDescr = this.log.fields.find(f => f.name === field.name);
                    this.recRenderer.renderField(e, r, field, fieldDescr);

                    this.fields.push({fieldName: field.name, html: e});
                }
            }
        }
    }

    fieldClick(event: MouseEvent) {
        this.recRenderer.handleClick(event);
    }

    ngAfterViewInit() {
        let fieldValues = this.fieldValues.toArray();

        LvUtils.assert(fieldValues.length === this.fields.length);

        for (let i = 0; i < this.fields.length; i++) {
            fieldValues[i].nativeElement.append(this.fields[i].html);
        }
    }
}
