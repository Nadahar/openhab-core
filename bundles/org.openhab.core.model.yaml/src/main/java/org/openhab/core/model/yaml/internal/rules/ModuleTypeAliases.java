package org.openhab.core.model.yaml.internal.rules;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;

@NonNullByDefault
public class ModuleTypeAliases { // TODO: (Nad) Heeader + JavaDocs

    private static final Map<Class<? extends Module>, Map<String, String>> ALIAS_IDX;
    private static final Map<Class<? extends Module>, Map<String, String>> TYPE_IDX;

    static {
        String[][] table = { //
            {"A", "EnableRule", "core.RuleEnablementAction"}, //
            {"A", "SendCommand", "core.ItemCommandAction"}, //
            {"A", "PostUpdate", "core.ItemStateUpdateAction"}, //
            {"A", "Play", "media.PlayAction"}, //
            {"A", "RunRule", "core.RunRuleAction"}, //
            {"A", "Say", "media.SayAction"}, //
            {"A", "Script", "script.ScriptAction"}, //
            {"C", "DayOfWeek", "timer.DayOfWeekCondition"}, //
            {"C", "Dayset", "ephemeris.DaysetCondition"}, //
            {"C", "Holiday", "ephemeris.HolidayCondition"}, //
            {"C", "Interval", "timer.IntervalCondition"}, //
            {"C", "ItemState", "core.ItemStateCondition"}, //
            {"C", "NotHoliday", "ephemeris.NotHolidayCondition"}, //
            {"C", "Script", "script.ScriptCondition"}, //
            {"C", "TimeOfDay", "core.TimeOfDayCondition"}, //
            {"C", "Weekday", "ephemeris.WeekdayCondition"}, //
            {"C", "Weekend", "ephemeris.WeekendCondition"}, //
            {"T", "ChannelEvent", "core.ChannelEventTrigger"}, //
            {"T", "Cron", "timer.GenericCronTrigger"}, //
            {"T", "DateTime", "timer.DateTimeTrigger"}, //
            {"T", "MemberReceivedCommand", "core.GroupCommandTrigger"}, //
            {"T", "MemberChanged", "core.GroupStateChangeTrigger"}, //
            {"T", "MemberUpdated", "core.GroupStateUpdateTrigger"}, //
            {"T", "ItemReceivedCommand", "core.ItemCommandTrigger"}, //
            {"T", "ItemChanged", "core.ItemStateChangeTrigger"}, //
            {"T", "ItemUpdated", "core.ItemStateUpdateTrigger"}, //
            {"T", "StartLevel", "core.SystemStartlevelTrigger"}, //
            {"T", "ThingChanged", "core.ThingStatusChangeTrigger"}, //
            {"T", "ThingUpdated", "core.ThingStatusUpdateTrigger"}, //
            {"T", "TimeOfDay", "timer.TimeOfDayTrigger"} //
        };

        Map<Class<? extends Module>, Map<String, String>> aliasIdx = new HashMap<>();
        Map<Class<? extends Module>, Map<String, String>> typeIdx = new HashMap<>();
        Class<? extends Module> clazz;
        Map<String, String> inner;
        for (String[] entry : table) {
            switch (entry[0]) {
                case "A":
                    clazz = Action.class;
                    break;
                case "C":
                    clazz = Condition.class;
                    break;
                case "T":
                    clazz = Trigger.class;
                    break;
                default:
                    continue;
            }
            if (!aliasIdx.containsKey(clazz)) {
                aliasIdx.put(clazz, new HashMap<>());
            }
            if (!typeIdx.containsKey(clazz)) {
                typeIdx.put(clazz, new HashMap<>());
            }
            if ((inner = aliasIdx.get(clazz)) != null) {
                inner.put(entry[1], entry[2]);
            }
            if ((inner = typeIdx.get(clazz)) != null) {
                inner.put(entry[2], entry[1]);
            }
        }
        ALIAS_IDX = Collections.unmodifiableMap(aliasIdx);
        TYPE_IDX = Collections.unmodifiableMap(typeIdx);
    }

    /**
     * Not to be instantiated
     */
    private ModuleTypeAliases() {
    }

    public static String aliasToType(Class<? extends Module> clazz, String alias) {
        Map<String, String> map = ALIAS_IDX.get(clazz);
        return map != null ? map.getOrDefault(alias, alias) : alias;
    }

    public static String typeToAlias(Class<? extends Module> clazz, String type) {
        Map<String, String> map = TYPE_IDX.get(clazz);
        return map != null ? map.getOrDefault(type, type) : type;
    }
}
