package de.johannesherr.myrunningapp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentHistory extends Fragment {


	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_history, container, false);

		final MainActivity activity = (MainActivity) getActivity();
		final List<CompletedRun> runs = activity.getRuns();

		final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy");

		ListView listView = view.findViewById(R.id.runListView);
		listView.setAdapter(new RunsAdapter(runs, activity, sdf));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				CompletedRun run = runs.get(position);
				MainActivity.RunInfo runInfo = MainActivity.getRunInfo(run);

				double avgSpeed = runInfo.dist / 1000.0 / runInfo.mins * 60;

				Toast.makeText(activity, String.format(Locale.US, "%.1f km/h", avgSpeed), Toast.LENGTH_SHORT).show();
			}
		});

		return view;
	}

	private static class RunsAdapter extends BaseAdapter {
		private final List<CompletedRun> runs;
		private final MainActivity activity;
		private final SimpleDateFormat sdf;

		public RunsAdapter(List<CompletedRun> runs, MainActivity activity, SimpleDateFormat sdf) {
			this.runs = runs;
			this.activity = activity;
			this.sdf = sdf;
		}

		@Override
		public int getCount() {
			return runs.size();
		}

		@Override
		public Object getItem(int position) {
			return runs.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView textView;
			if (convertView != null) {
				textView = (TextView) convertView;
			} else {
				textView = new TextView(activity);
			}

			CompletedRun run = runs.get(position);
			long timestamp = run.getTimestamp();

			String description = MainActivity.getRunDescription(sdf, run);

			textView.setText(description);
			return textView;
		}
	}
}
