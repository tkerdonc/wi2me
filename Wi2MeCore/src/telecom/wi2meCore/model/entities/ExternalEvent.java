/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meCore.model.entities;

import telecom.wi2meCore.model.entities.Trace.TraceType;


public class ExternalEvent extends Trace{

	public static final String TABLE_NAME = "ExternalEvent";
	public static final String EVENT = "event";

	private String event;

	protected ExternalEvent(Trace trace, String event){
		Trace.copy(trace, this);
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public static ExternalEvent getNewExternalEvent(Trace trace, String event){
		return new ExternalEvent(trace, event);
	}

	public String toString(){
		return super.toString() + "EXTERNAL_EVENT:" + event;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.EXTERNAL_EVENT;
	}
}
